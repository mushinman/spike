(ns org.spike.request
  (:require [org.spike.impl.request :as impl]))

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

  ## Notes:
  - If `:accept` is a string its value is set as the header.


  # Return value
  A map with the following format:
| Key             | Type                        | Meaning                                            |
|:----------------|:----------------------------|:---------------------------------------------------|
| `:res`          | Native response object      | The native response object returned by the request |
| `:status-code`  | int                         | HTTP status code                                   |
| `:body`         | Native response body object | The native body object returned by the request     |
| `:content-type` | string                      | The Content-Type header of the response            |
  "
  [context]
  (impl/send context))

(defn send-async
  [context]
  (impl/send-async context))
