(ns org.spike.mime)

(def json "application/json")
(def edn "application/edn")
(def text "text/plain")
(def form-data "application/x-www-form-urlencoded")
(def multipart "multipart/form-data")

(defn validate-content-type
  "Validate that content-type is a string. Returns the content-type or throws."
  [content-type]
  (if (string? content-type)
    content-type
    (throw (ex-info "content-type must be a string"
                    {:type :invalid-content-type
                     :content-type content-type}))))

(defn validate-accept
  "Validate that accept is a string. Returns the accept or throws."
  [accept]
  (if (string? accept)
    accept
    (throw (ex-info "accept must be a string"
                    {:type :invalid-accept-type
                     :accept accept}))))
