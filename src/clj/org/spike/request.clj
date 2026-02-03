(ns org.spike.request
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [promesa.core :as p]
            [cheshire.core :as json]
            [clojure.string :as str]
            [org.spike.util :refer [charset-utf8 get-str-bytes string->input-stream]]
            [lambdaisland.uri :refer [uri join assoc-query]]
            [org.spike.mime :as mime])
  (:import [java.util.function Supplier BiConsumer]
           [java.util Vector Collection Optional]
           [java.net.http HttpClient HttpClient$Version HttpRequest
            HttpRequest$BodyPublishers HttpRequest$BodyPublisher
            HttpResponse HttpResponse$BodyHandler HttpResponse$BodyHandlers
            HttpRequest$Builder HttpHeaders]
           [java.util.concurrent CompletableFuture]
           [java.nio.file Path Files]
           [java.net URI]
           [java.io SequenceInputStream PushbackReader]
           [java.net URLEncoder]
           [java.nio.charset StandardCharsets Charset]
           [java.time Duration Period]
           [java.io InputStream File]))

(def ^:private default-http-client
  "Default HTTP client. Is nil until first use. Must be .closed() if used."
  (atom nil))

(defn- get-default-client
  "Get the default client, initializing it if it does not exist."
  ^HttpClient
  []
  (or @default-http-client
      (swap! default-http-client (fn [c]
                                   (when c
                                     (.close ^HttpClient c))
                                   (-> (HttpClient/newBuilder)
                                       (.build))))))

(defn close-client
  "Close the default HTTP client if it exists."
  []
  (when @default-http-client
    (.close ^HttpClient @default-http-client)))

(defn coerce-to-file
  "If possible, return `p` coerced to a `java.io.File`, else `nil`."
  ^File
  [p]
  (cond
    (instance? File p)
    p

    (instance? Path p)
    (.toFile ^Path p)

    ;; java.net.URI
    (clojure.core/uri? p)
    (let [u ^URI p]
      (if (= (.getScheme u) "file")
        (File. u)
        (throw (ex-info "only file:// is supported" {:uri u}))))

    ;; Lambdaisland uri.
    (uri? p)
    (if (= (p :scheme) "file")
      (File. (URI. (str p)))
      (throw (ex-info "only file:// is supported" {:uri p})))

    :else nil))

(defn- url-encode
  "URL-encode a string."
  ^String
  [^String s]
  (URLEncoder/encode s charset-utf8))

(defn- create-form-string
  "Create a URL-encoded form body string. `form-body` must be assocable."
  [form-body]
  (str/join "&" (for [[k v] form-body]
                  (str (url-encode (str k)) "=" (url-encode (str v))))))

(defn- ->java-uri
  "Coerce `uri` to a `java.net.URI`."
  ^URI
  [uri]
  (URI. (str uri)))

(defn- add-headers
  "Take a clojure map of `string->string` and add each k-v pair as a header to the request builder."
  ^HttpRequest$Builder
  [^HttpRequest$Builder c headers]
  (doseq [[name value] headers]
    (.setHeader c (str name) (str value)))
  c)

(defn- get-content-type
  "Attempt to get the content type of a file, return `nil` on failure."
  [^File p]
  (Files/probeContentType (.toPath p)))


(defn- seq-stream
  "Convert a `Collection` or group of `InputStreams` into a `SequenceInputStream`."
  ^SequenceInputStream
  ([^Collection streams]
   (SequenceInputStream. ^Enumeration (->
                                       (Vector. streams)
                                       .elements)))
   ([stream1 stream2 & streams]
    (seq-stream (into [stream1 stream2] streams))))

(defn- create-multipart-boundary
  "Generate a multipart boundary string."
  []
  (str "----WebKitFormBoundary" (random-uuid)))

(defn- create-multipart-stream
  "Create an `InputStream` which is the body of a multipart request.

  # Arguments
  - `body`: A vector of maps. Each map must have `:content` and `name`, and optionally `content-type`,
  `file-name`, and `part-name`.
  - `boundary`: A mutlipart boundary string.

  # Return value
  An `InputStream` which is a valid multipart request body and contains all the contents of `body`."
  ^SequenceInputStream
  [body boundary]
  (let [[msg-streams _] ; Streams generated from the input body.
        (reduce
         (fn [[streams is-first] {:keys [content name] :as body-part}]
           (cond
             (coerce-to-file content)
             (let [content-path (coerce-to-file content)

                   {:keys [content-type file-name]
                    :or {content-type (get-content-type content-path)
                         file-name (.getName content-path)}}
                   body-part

                   header-buffer ; Header section as byte array.
                   (get-str-bytes
                    (str (when-not is-first "\r\n")
                         "--" boundary "\r\n"
                         "Content-Disposition: form-data; name=\"" name "\""
                         "; filename=\"" file-name "\"\r\n"
                         (when content-type
                           (str "Content-Type: " content-type))
                         "\r\n\r\n"))]

               [(conj streams (seq-stream ; Combine the header part and the file stream.
                               (io/input-stream header-buffer)
                               (io/input-stream content-path)))
                false])

             (instance? InputStream content)
             (let [{:keys [content-type file-name name]}
                   body-part

                   header-buffer ; Header section as byte array.
                   (get-str-bytes
                    (str (when-not is-first "\r\n")
                         "--" boundary "\r\n"
                         "Content-Disposition: form-data; name=\"" name "\""
                         (when file-name
                           (str "; filename=\"" file-name "\"\r\n"))
                         (when content-type
                           (str "Content-Type: " content-type))
                         "\r\n\r\n"))]

               [(conj streams (seq-stream ; Combine the header part and the file stream.
                               (io/input-stream header-buffer)
                               content))
                false])

             (string? content)
             (let [{:keys [content-type name]}
                   body-part

                   header-buffer ; Header section as byte array.
                   (get-str-bytes
                    (str (when-not is-first "\r\n")
                         "--" boundary "\r\n"
                         "Content-Disposition: form-data; name=\"" name "\""
                         (when content-type
                           (str "Content-Type: " content-type))
                         "\r\n\r\n"))]

               [(conj streams (seq-stream ; Combine the header part and the file stream.
                               (io/input-stream header-buffer)
                               (io/input-stream (get-str-bytes content))))
                false])))
         ;; First position is a vector of inputstreams.
         ;; Second is true if the current stream is the first
         ;; stream in the vector, false otherwise.
         [[] true]
         body)

        end-msg-buffer
        (get-str-bytes (str "\r\n--" boundary "--"))]
    (seq-stream (conj msg-streams (io/input-stream end-msg-buffer)))))

(defn- build-request
  "Build an HTTPRequest based off the context map."
  ^HttpRequest
  [{:keys [query accept-language body
           client base-uri method location
           version timeout accept content-type
           headers]
    :or {content-type mime/json
         accept mime/json}
    :as http-context}]
  (let [multipart-boundary (create-multipart-boundary)
        body-publisher ^HttpRequest$BodyPublisher
        (cond
          (nil? body) HttpRequest$BodyPublishers/noBody
          (instance? HttpRequest$BodyPublisher body) body
          (string? body) (HttpRequest$BodyPublishers/ofString ^String body)
          (instance? Supplier body) (HttpRequest$BodyPublishers/ofInputStream ^Supplier body)

          (instance? InputStream body)
          (HttpRequest$BodyPublishers/ofInputStream ^Supplier
                                                    (proxy [Supplier] []
                                                      (get [_] body)))

          (instance? Path body) (HttpRequest$BodyPublishers/ofInputStream ^Path body)

          (bytes? body) (HttpRequest$BodyPublishers/ofByteArray ^bytes body)

          ;; We encode ourselves according to the content-type.
          :else
          (let [ct-str (mime/validate-content-type content-type)]
            (condp = ct-str
              mime/json (HttpRequest$BodyPublishers/ofString (json/generate-string body))
              mime/edn (HttpRequest$BodyPublishers/ofString (pr-str body))
              mime/multipart (HttpRequest$BodyPublishers/ofInputStream
                              ^Supplier
                              (proxy [Supplier] []
                                (get [_] (create-multipart-stream body multipart-boundary))))
              mime/form-data (HttpRequest$BodyPublishers/ofString (create-form-string body))
              (throw (ex-info "can't serialize body; unknown content-type"
                              {:type :invalid-content-type
                               :content-type content-type})))))]
    (.build
     (cond->
         (doto (HttpRequest/newBuilder)
           (.uri
            (->java-uri
             (assoc-query (if base-uri
                            (join base-uri location)
                            (uri location))
                          query))))
       version
       (.version ^HttpClient$Version
                 (case version
                   :1.1 HttpClient$Version/HTTP_1_1
                   :2.0 HttpClient$Version/HTTP_2
                   (throw (ex-info "invalid HTTP version (only supports 1.1 and 2.0)"
                                   {:type :invalid-http-version
                                    :value version}))))

       timeout
       (.timeout
        ;; Coerce to a Duration.
        (cond
          (instance? Duration timeout) timeout
          ;; Assumed milliseconds.
          (number? timeout) (Duration/ofMillis ^long (long timeout))
          :else (throw (ex-info "invalid type for timeout"
                                {:type :invalid-timeout-type
                                 :timeout timeout}))))

       headers
       (add-headers headers)

       accept-language
       (.setHeader "Accept-Language" ^String (str accept-language))

       content-type
       (.setHeader "Content-Type" ^String (let [ct-str (mime/validate-content-type content-type)]
                                            (if (= ct-str mime/multipart)
                                              (str ct-str "; boundary=" multipart-boundary)
                                              ct-str)))

       accept
       (.setHeader "Accept" ^String (mime/validate-accept accept))

       ;; Method.
       (= method :get)
       (.GET)

       (= method :post)
       (.POST body-publisher)

       (= method :delete)
       (.DELETE)

       (= method :patch)
       (.method "PATCH" body-publisher)

       (= method :put)
       (.PUT body-publisher)

       (= method :head)
       (.method "HEAD" body-publisher)))))


(defn- get-header-value
  ^String
  [^HttpHeaders headers ^String name]
  (let [v (.firstValue headers name)]
      (when (.isPresent v)
        (.get v))))

(defn- process-response
  [^HttpResponse http-response]
  {:res http-response
   :status-code (.statusCode http-response)
   :body (.body http-response)
   :content-type (get-header-value (.headers http-response) "Content-Type")})

(defn send
  "Synchronously send an HTTP request based off of a context map. Returns a response map.

  # Arguments
  - `context`: A map of the following format:
| Key                | Type                  | Meaning                                                                                                                             |
|:-------------------|:----------------------|:------------------------------------------------------------------------------------------------------------------------------------|
| `:location`        | URI or string         | (Required) If `:base-uri` is provided both values are combined to create the final request URI, else `:location` is the request URI |
| `:method`          | keyword               | (Required) An HTTP method. One of: `:get`, `:post`, `:delete`, `:put`, `:patch`, `:head`                                            |
| `:version`         | keyword               | (Optional) An HTTP version to use. One of: `1.1`, `2.0`                                                                             |
| `:timeout`         | Time object or number | (Optional) HTTP request timeout. Can be a native time duration object. If a number is assumed to be milliseconds                    |
| `:base-uri`        | URI or string         | (Optional) If provided, is prepended to `location` to create the final request URI                                                  |
| `:client`          | Native HTTP client    | (Optional) A native HTTP client instance                                                                                            |
| `:body`            | Any                   | (Optional) The request body. Meaning depends on `content-type`                                                                      |
| `:query`           | Map                   | (Optional) Query arguments to append to the URI                                                                                     |
| `:accept`          | keyword or string     | (Optional) Accept header. If a keyword: one of `:json`, `:edn`, `:text`                                                             |
| `:content-type`    | keyword or string     | (Optional) Content-Type header. One of `:json`, `:edn`, `:text`, `:multipart`, `:form`                                              |
| `:accept-language` | string                | (Optional) Accept-Language header value                                                                                             |
| `:headers`         | Map                   | (Optional) Map of headers                                                                                                           |

  ## Notes:
  - If `:accept` is a string its value is set as the header.


  # Return value
  A map with the following format:
| Key             | Type                        | Meaning                                            |
|:----------------|:----------------------------|:---------------------------------------------------|
| `:res`          | Native response object      | The native response object returned by the request |
| `:status-code`  | int                         | HTTP status code                                   |
| `:body`         | Native response body object | The native body object returned by the request     |
| `:content-type` | string                      | The Content-Type header of the response            |
  "
  [{:keys [client]
    :as http-context}]
  (let [client (or client
                   (get-default-client))
        http-response (.send ^HttpClient client
                             ^HttpRequest (build-request http-context)
                             (HttpResponse$BodyHandlers/ofInputStream))]
    (process-response http-response)))

(defn send-async
  "Asynchronously send an HTTP request based off of a context map. Returns a channel."
  [{:keys [client]
    :as http-context}]
  (let [client (or client
                   (get-default-client))]
    (-> (p/promise (.sendAsync ^HttpClient client
                           ^HttpRequest (build-request http-context)
                           (HttpResponse$BodyHandlers/ofInputStream)))
        (p/then process-response))))

(defn get
  ([location query http-context]
   (send (assoc http-context
                :location location
                :method :get
                :query query)))
  ([location query]
   (get location query {}))
  ([location]
   (get location {} {})))

(defn get-async
  ([location query http-context]
   (send-async (assoc http-context
                      :location location
                      :method :get
                      :query query)))
  ([location query]
   (get-async location query {}))
  ([location]
   (get-async location {} {})))

(defn post
  ([location body query http-context]
   (send (assoc http-context
                :body body
                :location location
                :method :post
                :query query)))
  ([location body query]
   (post location body query {}))
  ([location body]
   (post location body {} {})))
