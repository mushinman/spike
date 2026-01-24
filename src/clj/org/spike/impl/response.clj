(ns org.spike.impl.response
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.core.async :refer [<! chan close!]]
            [org.spike.impl.util :refer [charset-utf8 get-str-bytes]]
            [lambdaisland.uri :refer [uri join assoc-query]])
  (:import [java.util.function Supplier BiConsumer]
           [java.util Vector Collection]
           [java.net.http HttpClient HttpClient$Version HttpRequest
            HttpRequest$BodyPublishers HttpRequest$BodyPublisher
            HttpResponse HttpResponse$BodyHandler HttpResponse$BodyHandlers
            HttpRequest$Builder]
           [java.nio.file Path Files]
           [java.net URI]
           [java.io SequenceInputStream PushbackReader]
           [java.net URLEncoder]
           [java.nio.charset StandardCharsets Charset]
           [java.time Duration Period]
           [java.io InputStream ByteArrayOutputStream]))

(defn- bytes->string
  "Convert a byte array to a string (assumes UTF8 encoding)."
  ^String
  [^bytes b]
  (String. b charset-utf8))

(defn to-string
  "Transform the body of `res` into a string."
  ^String
  [{:keys [body] :as res}]
  (if (string? body)
    body
    (cond
      (instance? InputStream body)
      (with-open [reader (PushbackReader. (io/reader body))]
        (slurp reader))

      (bytes? body)
      (bytes->string body))))

(defn to-stream
  "Transform the body of `res` into a InputStream."
  ^InputStream
  [{:keys [body] :as res}]
  (cond
    (instance? InputStream body) body

    (string? body) (io/input-stream (get-str-bytes body))

    (bytes? body) (io/input-stream body)))

(defn to-bytes
  "Transform the body of `res` into a byte array."
  ^bytes
  [{:keys [body] :as res}]
  (cond
    (bytes? body) body

    (instance? InputStream body)
    (with-open [os (ByteArrayOutputStream.)
                body ^InputStream body]
      (io/copy body os)
      (.toByteArray os))

    (string? body) (get-str-bytes body)))
