(ns custom-handlers
  (:require
    [framework.components.app.core :as xiana-app]))

(defn post-handler
  [state behavior]
  (xiana-app/default-handler
    (assoc state :behavior behavior)))
