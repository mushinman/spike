(ns org.spike.response
  (:require #?(:clj [org.spike.impl.response :as impl])))

(defn assert-success!
  "Throw if status-code is not a success code, else return `res`."
  [{:keys [status-code] :as res}]
  (if (and (>= status-code 200)
           (<= status-code 299))
    res
    (throw (ex-info {:reason :failure-status-code
                     :status-code status-code}))))

(defn to-string
  "Transform the body of `res` into a string."
  [res]
  (impl/to-string res))

(defn to-stream
  "Transform the body of `res` into a InputStream."
  [res]
  (impl/to-stream res))

(defn to-bytes
  "Transform the body of `res` into a byte array."
  [res]
  (impl/to-bytes res))
