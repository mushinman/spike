(ns social.mushin.spike.request
  (:require #?(:clj [social.mushin.spike.request.impl :as impl])
            #?(:cljs [social.mushin.spike.request.impl :as impl])))

(defn get-context
  "Create a http context object for a GET request, using another context object as a base."
  ([http-context location query]
   (assoc http-context
          :location location
          :method :get
          :query query))
  ([http-context location]
   (get-context http-context location {})))

(defn head-context
  "Create a http context object for a HEAD request, using another context object as a base."
  ([http-context location query]
   (assoc http-context
          :location location
          :method :head
          :query query))
  ([http-context location]
   (head-context http-context location {})))

(defn delete-context
  "Create a http context object for a DELETE request, using another context object as a base."
  ([http-context location query]
   (assoc http-context
          :location location
          :method :delete
          :query query))
  ([http-context location]
   (delete-context http-context location {})))

(defn post-context
  "Create a http context object for a POST request, using another context object as a base."
  ([http-context location body query]
   (assoc http-context
          :location location
          :body body
          :method :post
          :query query))
  ([http-context location body]
   (post-context http-context location body {}))
  ([http-context location]
   (post-context http-context location {} {})))

(defn put-context
  "Create a http context object for a PUT request, using another context object as a base."
  ([http-context location body query]
   (assoc http-context
          :location location
          :body body
          :method :put
          :query query))
  ([http-context location body]
   (put-context http-context location body {}))
  ([http-context location]
   (put-context http-context location {} {})))

(defn patch-context
  "Create a http context object for a PATCH request, using another context object as a base."
  ([http-context location body query]
   (assoc http-context
          :location location
          :body body
          :method :patch
          :query query))
  ([http-context location body]
   (patch-context http-context location body {}))
  ([http-context location]
   (patch-context http-context location {} {})))

#?(:clj
   (do
     (defn send
     "Synchronously send an HTTP request based off of a context map. Returns a response map.

  # Arguments
  - `context`: A map of the following format:
| Key                | Type                  | Meaning                                                                                                                             |
|:-------------------|:----------------------|:------------------------------------------------------------------------------------------------------------------------------------|
| `:location`        | URI or string         | (Required) If `:base-uri` is provided both values are combined to create the final request URI, else `:location` is the request URI |
| `:method`          | keyword               | (Required) An HTTP method. One of: `:get`, `:post`, `:delete`, `:put`, `:patch`, `:head`                                            |
| `:version`         | keyword               | (Optional) An HTTP version to use. One of: `1.1`, `2.0`                                                                             |
| `:timeout`         | Time object or number | (Optional) HTTP request timeout. Can be a native time duration object. If a number is assumed to be milliseconds                    |
| `:base-uri`        | URI or string         | (Optional) If provided, is prepended to `location` to create the final request URI                                                  |
| `:client`          | Native HTTP client    | (Optional) A native HTTP client instance                                                                                            |
| `:body`            | Any                   | (Optional) The request body. Meaning depends on `content-type`                                                                      |
| `:query`           | Map                   | (Optional) Query arguments to append to the URI                                                                                     |
| `:accept`          | keyword or string     | (Optional) Accept header. If a keyword: one of `:json`, `:edn`, `:text`                                                             |
| `:content-type`    | keyword or string     | (Optional) Content-Type header. One of `:json`, `:edn`, `:text`, `:multipart`, `:form`                                              |
| `:accept-language` | string                | (Optional) Accept-Language header value                                                                                             |
| `:headers`         | Map                   | (Optional) Map of headers                                                                                                           |
| `:authorization`   | Tuple                 | (Optional) Tuple of a type and payload.                                                                                             |
| `:api-key`         | String                | (Optional) API key.                                                                                                                 |

  ## Notes:
  - If `:accept` is a string its value is set as the header.
  - `:authorization` is a tuple with a type and payload. Type can be `:basic` or `:bearer`.
      If `:bearer`, the second value should be a string.
      If `:basic`, the second value should be a map containing a `:username` and a `:password`.


  # Return value
      A map with the following format:
| Key             | Type                        | Meaning                                            |
|:----------------|:----------------------------|:---------------------------------------------------|
| `:res`          | Native response object      | The native response object returned by the request |
| `:status-code`  | int                         | HTTP status code                                   |
| `:body`         | Native response body object | The native body object returned by the request     |
| `:content-type` | string                      | The Content-Type header of the response            |
     "
     [http-context]
     (impl/send http-context))

   (defn get
     "Make a synchronous GET request to `location`. Optionally with a `query`.

      Also optionally with a `http-context`.

      Returns a spike response object."
     ([location query http-context]
      (send (get-context http-context location query)))
     ([location query]
      (get location query {}))
     ([location]
      (get location {} {})))


   (defn post
     "Make a synchronous POST request to `location`. Optionally with a `body` and a `query`.

      Also optionally with a `http-context`.

      Returns a spike response object."
     ([location body query http-context]
      (send (post-context http-context location body query)))
     ([location body query]
      (post location body query {}))
     ([location body]
      (post location body {} {})))


   (defn head
     "Make a synchronous HEAD request to `location`. Optionally with a `query`.

      Also optionally with a `http-context`.

      Returns a spike response object."
     ([location query http-context]
      (send (head-context http-context location query)))
     ([location query]
      (head location query {}))
     ([location]
      (head location {} {})))

   (defn put
     "Make a synchronous PUT request to `location`. Optionally with a `body` and a `query`.

      Also optionally with a `http-context`.

      Returns a spike response object."
     ([location body query http-context]
      (send (put-context http-context location body query)))
     ([location body query]
      (put location body query {}))
     ([location body]
      (put location body {} {})))


   (defn patch
     "Make a synchronous PATCH request to `location`. Optionally with a `body` and a `query`.

      Also optionally with a `http-context`.

      Returns a spike response object."
     ([location body query http-context]
      (send (patch-context http-context location body query)))
     ([location body query]
      (patch location body query {}))
     ([location body]
      (patch location body {} {})))

   (defn delete
     "Make a DELETE request to `location`. Optionally with a `query`.

      Also optionally with a `http-context`.

      Returns a spike response object."
     ([location query http-context]
      (send (delete-context http-context location query)))
     ([location query]
      (delete location query {}))
     ([location]
      (delete location {} {})))))

(defn send-async
  "Asynchronously send an HTTP request based off of a context map. Returns a promise.

  # Arguments
  - `context`: A map of the following format:
| Key                | Type                  | Meaning                                                                                                                             |
|:-------------------|:----------------------|:------------------------------------------------------------------------------------------------------------------------------------|
| `:location`        | URI or string         | (Required) If `:base-uri` is provided both values are combined to create the final request URI, else `:location` is the request URI |
| `:method`          | keyword               | (Required) An HTTP method. One of: `:get`, `:post`, `:delete`, `:put`, `:patch`, `:head`                                            |
| `:version`         | keyword               | (Optional) An HTTP version to use. One of: `1.1`, `2.0`                                                                             |
| `:timeout`         | Time object or number | (Optional) HTTP request timeout. Can be a native time duration object. If a number is assumed to be milliseconds                    |
| `:base-uri`        | URI or string         | (Optional) If provided, is prepended to `location` to create the final request URI                                                  |
| `:client`          | Native HTTP client    | (Optional) A native HTTP client instance                                                                                            |
| `:body`            | Any                   | (Optional) The request body. Meaning depends on `content-type`                                                                      |
| `:query`           | Map                   | (Optional) Query arguments to append to the URI                                                                                     |
| `:accept`          | keyword or string     | (Optional) Accept header. If a keyword: one of `:json`, `:edn`, `:text`                                                             |
| `:content-type`    | keyword or string     | (Optional) Content-Type header. One of `:json`, `:edn`, `:text`, `:multipart`, `:form`                                              |
| `:accept-language` | string                | (Optional) Accept-Language header value                                                                                             |
| `:headers`         | Map                   | (Optional) Map of headers                                                                                                           |
| `:authorization`   | Tuple                 | (Optional) Tuple of a type and payload.                                                                                             |
| `:api-key`         | String                | (Optional) API key.                                                                                                                 |

  ## Notes:
  - If `:accept` is a string its value is set as the header.
  - `:authorization` is a tuple with a type and payload. Type can be `:basic` or `:bearer`.
      If `:bearer`, the second value should be a string.
      If `:basic`, the second value should be a map containing a `:username` and a `:password`.


  # Return value
  A map with the following format:
| Key             | Type                        | Meaning                                            |
|:----------------|:----------------------------|:---------------------------------------------------|
| `:res`          | Native response object      | The native response object returned by the request |
| `:status-code`  | int                         | HTTP status code                                   |
| `:body`         | Native response body object | The native body object returned by the request     |
| `:content-type` | string                      | The Content-Type header of the response            |
  "
  [http-context]
  (impl/send-async http-context))


(defn get-async
  "Make a GET request to `location`. Optionally with a `query`.

   Also optionally with a `http-context`.

   Returns a promise to a spike response object."
  ([location query http-context]
   (send-async (get-context http-context location query)))
  ([location query]
   (get-async location query {}))
  ([location]
   (get-async location {} {})))

(defn head-async
  "Make a HEAD request to `location`. Optionally with a `query`.

   Also optionally with a `http-context`.

   Returns a promise to a spike response object."
  ([location query http-context]
   (send-async (head-context http-context location query)))
  ([location query]
   (head-async location query {}))
  ([location]
   (head-async location {} {})))

(defn post-async
  "Make a POST request to `location`. Optionally with a `body` and a `query`.

   Also optionally with a `http-context`.

   Returns a promise to a spike response object."
  ([location body query http-context]
   (send-async (post-context http-context location body query)))
  ([location body query]
   (post-async location body query {}))
  ([location body]
   (post-async location body nil {})))

(defn put-async
  "Make a PUT request to `location`. Optionally with a `body` and a `query`.

   Also optionally with a `http-context`.

   Returns a promise to a spike response object."
  ([location body query http-context]
   (send-async (put-context http-context location body query)))
  ([location body query]
   (put-async location body query {}))
  ([location body]
   (put-async location body {} {})))

(defn patch-async
  "Make a PATCH request to `location`. Optionally with a `body` and a `query`.

   Also optionally with a `http-context`.

   Returns a promise to a spike response object."
  ([location body query http-context]
   (send-async (patch-context http-context location body query)))
  ([location body query]
   (patch-async location body query {}))
  ([location body]
   (patch-async location body {} {})))

(defn delete-async
  "Make a DELETE request to `location`. Optionally with a `query`.

   Also optionally with a `http-context`.

   Returns a promise to a spike response object."
  ([location query http-context]
   (send-async (delete-context http-context location query)))
  ([location query]
   (delete-async location query {}))
  ([location]
   (delete-async location {} {})))
