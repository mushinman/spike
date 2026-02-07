(ns social.mushin.spike.util
  (:require [clojure.java.io :as io])
  (:import [java.nio.charset StandardCharsets Charset]))

(def ^Charset charset-utf8
  "UTF8 charset."
  StandardCharsets/UTF_8)

(defn get-str-bytes
  "Convert a string to a UTF8-encoded byte array."
  ^bytes
  [^String s]
  (.getBytes s charset-utf8))

(defn string->input-stream
  "Convert a string to an `InputStream` that points to a UTF8 encoded buffer."
  [^String s]
  (io/input-stream (get-str-bytes s)))
