(ns custom-handlers
  (:require
    [framework.components.app.core :as xiana-app]))

(defn post-handler
  [state]
  (println "custom handler:" state)
  (xiana-app/default-handler state))
