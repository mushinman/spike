(ns org.spike.core
  (:require [org.spike.request :as rq]
            [org.spike.response :as res]))

(defn ^:export init []
  (js/console.log "Spike initialized"))
