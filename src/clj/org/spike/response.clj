(ns org.spike.response
  (:require [clojure.java.io :as io]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [org.spike.util :refer [charset-utf8]])
  (:import  [java.io PushbackReader Reader]))


(defn- bytes->string
  "Convert a byte array to a string (assumes UTF8 encoding)."
  ^String
  [^bytes b]
  (String. b charset-utf8))

(defn- is-pushbackreader?
  "Returns true if `o` is a PushbackReader, false if not."
  [o]
  (instance? PushbackReader o))

(defn- coerce-to-pushbackreader
  "Coerce `o` into a PushbackReader. `o` must already be a reader or convertable into a stream."
  ^PushbackReader
  [o]
  (cond
    (is-pushbackreader? o) o

    (instance? Reader o) (PushbackReader. o)

    :else (PushbackReader. (io/reader (io/input-stream o)))))


(defn read-response-text
  "Consume the entire response body, placing a string read from the body into the response object.

  Assumes that `body` is already a reader, or coercable to a stream."
  [{:keys [body] :as res}]
  (assoc res :body (slurp (coerce-to-pushbackreader body))))

(defn read-response-edn
  "Consume the entire response body, placing a vector of objects parsed from the response body into the response object.

  Assumes that `body` is already a reader, or coercable to a stream."
  ([{:keys [body] :as res}
    {:keys [eof] :as opts}]
   (let [eof-sentinel (or eof ::eof)
         opts (assoc opts :eof eof-sentinel)]
     (with-open [r (coerce-to-pushbackreader body)] 
       (assoc res :body (into [] (take-while (fn [o]
                                               (not= o eof-sentinel))
                                             (repeatedly (edn/read r opts))))))))
  ([res]
   (read-response-edn res {:eof ::eof})))

(defn read-response-json
  "Consume the entire response body, placing an object read from the body into the response object.

  Assumes that `body` is already a reader, or coercable to a stream."
  [{:keys [body] :as res}]
  (with-open [pbr (coerce-to-pushbackreader body)]
    (assoc res :body (json/parse-stream pbr))))


