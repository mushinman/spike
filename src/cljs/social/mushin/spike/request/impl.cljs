(ns social.mushin.spike.request.impl
  (:require [clojure.edn :as edn]
            [promesa.core :as p]
            [lambdaisland.uri :refer [uri join assoc-query]]
            [social.mushin.spike.mime :as mime]))

(defn- readable-stream?
  [obj]
  (instance? js/ReadableStream obj))

(defn- form-data?
  [obj]
  (instance? js/FormData obj))


(defn send-async
  "Asynchronously send an HTTP request based off of a context map. Returns a channel.

  # Arguments
  - `context`: A map of the following format:
| Key                | Type                  | Meaning                                                                                                                             |
|:-------------------|:----------------------|:------------------------------------------------------------------------------------------------------------------------------------|
| `:location`        | URI or string         | (Required) If `:base-uri` is provided both values are combined to create the final request URI, else `:location` is the request URI |
| `:method`          | keyword               | (Required) An HTTP method. One of: `:get`, `:post`, `:delete`, `:put`, `:patch`, `:head`                                            |
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
  [{:keys [query accept-language body
           base-uri method location
           timeout accept content-type
           headers]
    :or {accept mime/json
         content-type mime/json}
    :as http-context}]
  (let [content-type-str (when content-type (mime/validate-content-type content-type))
        accept-str (when accept (mime/validate-accept accept))]
    (-> (js/fetch (str (assoc-query
                        (if base-uri
                          (join base-uri location)
                          (uri location))
                        query))
                  (clj->js
                   {"method" "GET"}
                   (cond-> {"method"
                            (case method
                              :get "GET"
                              :post "POST"
                              :put "PUT"
                              :patch "PATCH"
                              :delete "DELETE"
                              :head "HEAD")

                            "headers"
                            (cond-> (or headers {})
                              accept-language (assoc "Accept-Language" (str accept-language))
                              accept-str (assoc "Accept" accept-str)
                              content-type-str (assoc "Content-Type" content-type-str))

                            ;; TODO check if string is readablestream
                            ;; add readablestream to possible body types
                            "body"
                            (condp = content-type-str
                              mime/json
                              (cond
                                (string? body) body
                                (readable-stream? body) body
                                :else (js/JSON.stringify (clj->js body)))

                              mime/edn
                              (cond
                                (string? body) body
                                (readable-stream? body) body
                                :else (pr-str body))

                              mime/text (str body)

                              mime/form-data
                              (if (form-data? body)
                                body
                                (reduce
                                 (fn [form-data [k v]]
                                   (.append form-data (if (keyword? k) (name k) (str k)) (str v))
                                   form-data)
                                 (js/FormData.)
                                 body))

                              mime/multipart
                              (if (form-data? body)
                                body
                                (reduce
                                 (fn [form-data {:keys [content name file-name]}]
                                   (if file-name
                                     (.append form-data name content file-name)
                                     (.append form-data name content))
                                   form-data)
                                 (js/FormData.)
                                 body)))

                            timeout
                            (js/AbortSignal.timeout timeout)})))
        p/promise
        (p/then (fn [response]
                  {:res response
                   :body (.-body response)
                   :status-code (.-status response)
                   :content-type (.get (.-headers response) "Content-Type")})))))

