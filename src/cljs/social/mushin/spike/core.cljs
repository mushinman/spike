(ns social.mushin.spike.core
  (:require [social.mushin.spike.request :as rq]
            [social.mushin.spike.response :as res]))

(defn ^:export init []
  (js/console.log "Spike initialized"))
