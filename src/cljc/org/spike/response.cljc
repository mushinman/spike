(ns org.spike.response)
            
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
