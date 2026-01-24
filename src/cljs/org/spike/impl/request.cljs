(ns org.spike.impl.request
  (:require [clojure.edn :as edn]
            [clojure.core.async :refer [put! chan close!]]
            [lambdaisland.uri :refer [uri uri? join assoc-query]]))

(defn- readable-stream?
  [obj]
  (instance? js/ReadableStream obj))

(defn- form-data?
  [obj]
  (instance? js/FormData obj))


(defn send-async
  "Asynchronously send an HTTP request based off of a context map. Returns a channel."
  [{:keys [query accept-language body
           base-uri method location
           timeout accept content-type
           headers]
    :as http-context}]
  (let [c (chan 1)]
    (-> (js/fetch (str (assoc-qeury
                        (if base-uri
                          (join base-uri location)
                          (uri location)))
                       query)
                  (clj->js
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
                              accept-language (assoc "Accept-Language" accept-language)
                              accept (assoc "Accept" accept)
                              content-type (assoc "Content-Type" content-type))

                            ;; TODO check if string is readablestream
                            ;; add readablestream to possible body types
                            "body"
                            (case content-type
                              "application/json"
                              (cond
                                (string? body) body
                                (readable-stream? body) body
                                :else (-> (clj->js body)
                                          js/JSON
                                          stringify))

                              "application/edn"
                              (cond
                                (string? body) body
                                (readable-stream? body) body
                                :else (pr-str body))

                              "text/plain" (str body)

                              "application/x-www-form-urlencoded"
                              (if (form-data? body)
                                body
                                (reduce
                                 (fn [form-data [k v]]
                                   (.append (if (keyword? k) (name k) (str k)) (str v)))
                                 (js/FormData.)
                                 body))

                              "multipart/form-data"
                              (if (form-data? body)
                                body
                                (reduce
                                 (fn [form-data {:keys [content name file-name]}]
                                   (if file-name
                                     (.append name content file-name)
                                     (.append name content)))
                                 (js/FormData.)
                                 body)))

                            timeout
                            "signal" (js/AbortSignal.timeout timeout)}

                     )))
        (.then (fn [response]
                 (put! c {:res response
                          :body (.-body response)
                          :status-code (.-status response)})
                 (close! c)))
        (.catch (fn [err]
                  (put! c {:error err
                           :context http-context})
                  (close! c))))
    c))
