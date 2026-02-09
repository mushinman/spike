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
  "Asynchronously send an HTTP request. Returns a promise to a spike response object.

   See `social.mushin.spike.request.send-async` for more details."
  [{:keys [query accept-language body
           base-uri method location
           timeout accept content-type
           headers authorization api-key]
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
                   (cond->
                    {"method"
                     (case method
                       :get "GET"
                       :post "POST"
                       :put "PUT"
                       :patch "PATCH"
                       :delete "DELETE"
                       :head "HEAD"
                       (throw (if method
                                (ex-info "Invalid HTTP method"
                                         {:method method})
                                (ex-info "No HTTP method provided"
                                         {}))))
                     "headers"
                     (cond-> (or headers {})
                       accept-language (assoc "Accept-Language" (str accept-language))
                       accept-str (assoc "Accept" accept-str)
                       content-type-str (assoc "Content-Type" content-type-str)
                       authorization (assoc "Authorization"
                                            (if (vector? authorization)
                                              ;; Tuple format, get the type of auth from the first arg.
                                              (let [[type payload] authorization]
                                                (case type
                                                  :basic (str "Basic " (js/btoa (str (:username payload) ":" (:password payload))))
                                                  :bearer (str "Bearer " payload)))
                                              (str authorization)))
                       api-key (assoc "X-API-KEY" (str api-key)))}

                     ;; TODO check if string is readablestream
                     ;; add readablestream to possible body types
                     body
                     (assoc "body"
                            (condp = content-type-str
                              mime/json
                              (if (or (string? body)
                                      (readable-stream? body))
                                body
                                (js/JSON.stringify (clj->js body)))

                              mime/edn
                              (if (or (string? body)
                                      (readable-stream? body))
                                body
                                (pr-str body))

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
                                 body))))
                     timeout
                     (assoc "timeout" (js/AbortSignal.timeout timeout)))))
        p/promise
        (p/then (fn [response]
                  {:res response
                   :body (.-body response)
                   :status-code (.-status response)
                   :content-type (.get (.-headers response) "Content-Type")})))))

