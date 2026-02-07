(ns social.mushin.spike.response
  (:require #?(:clj [social.mushin.spike.response.impl :as impl])
            #?(:cljs [social.mushin.spike.response.impl :as impl])))
            
(defn body
  "Get the body of a `response` object."
  [{:keys [body]}]
  body)

(defn is-success?
  "Return true if `status-code` is a success code, false if not."
  [{:keys [status-code]}]
  (and (>= status-code 200)
       (<= status-code 299)))

(defn assert-success!
  "Throw if status-code is not a success code, else return `res`."
  [{:keys [status-code] :as res}]
  (if (is-success? res)
    res
    (throw (ex-info "Failure status code"
                    (cond-> {:reason :failure-status-code
                             :status-code status-code
                             :res res}
                      (body res) (assoc :body body))))))

;;; Platform-specific functions (delegate to impl).

#?(:clj
   (defn read-response-text
     "Consume the entire response body, placing a string read from the body into the response object.

  Assumes that `body` is already a reader, or coercable to a stream."
     [res]
     (impl/read-response-text res)))

#?(:clj
   (defn read-response-edn
     "Consume the entire response body, placing a vector of objects parsed from the response body into the response object.

  Assumes that `body` is already a reader, or coercable to a stream."
     ([res opts]
      (impl/read-response-edn res opts))
     ([res]
      (impl/read-response-edn res))))

#?(:clj
   (defn read-response-json
     "Consume the entire response body, placing an object read from the body into the response object.

  Assumes that `body` is already a reader, or coercable to a stream."
     [res]
     (impl/read-response-json res)))

#?(:cljs
   (defn read-response-json-async
     "Consume the entire response body, placing an object read from the body into the response object.

  Assumes that `body` is already a reader, or coercable to a stream.

  Returns a promise to a new response object."
     [response]
     (impl/read-response-json-async response)))

#?(:cljs
   (defn read-response-text-async
     "Consume the entire response body, placing a string read from the body into the response object.

  Assumes that `body` is already a reader, or coercable to a stream.

  Returns a promise to a new response object."
     [response]
     (impl/read-response-text-async response)))

#?(:cljs
   (defn read-response-edn-async
     "Consume the entire response body, placing a vector of objects parsed from the response body into the response object.

  Assumes that `body` is already a reader, or coercable to a stream.

  Returns a promise to a new response object."
     ([response opts]
      (impl/read-response-edn-async response opts))
     ([response]
      (impl/read-response-edn-async response))))
