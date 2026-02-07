(ns social.mushin.spike.response.impl
  (:require [clojure.edn :as edn]
            [cljs.tools.reader.reader-types :as readers]
            [promesa.core :as p]))

(defn read-response-json-async
  "Consume the entire response body, placing an object read from the body into the response object.

  Assumes that `body` is already a reader, or coercable to a stream.

  Returns a promise to a new response object."
  [{:keys [res] :as response}]
  (-> (p/promise (.json res))
      (p/then (fn [body] (assoc response :body (js->clj body))))))

(defn read-response-text-async
  "Consume the entire response body, placing a string read from the body into the response object.

  Assumes that `body` is already a reader, or coercable to a stream.

  Returns a promise to a new response object."
  [{:keys [res] :as response}]
  (-> (p/promise (.text res))
      (p/then (fn [body] (assoc response :body body)))))

(defn read-response-edn-async
  "Consume the entire response body, placing a vector of objects parsed from the response body into the response object.

  Assumes that `body` is already a reader, or coercable to a stream.

  Returns a promise to a new response object."
  ([{:keys [res] :as response}
    {:keys [eof] :as opts}]
   (let [eof-sentinel (or eof ::eof)
         opts (assoc opts :eof eof-sentinel)]
     (-> (p/promise (.text res))
         (p/chain readers/indexing-push-back-reader
                  (fn [pbr]
                    (assoc response
                           :body
                           (into [] (take-while (fn [o]
                                                  (not= o eof-sentinel))
                                                (repeatedly (edn/read opts pbr))))))))))
  ([res]
   (read-response-edn-async res {})))
