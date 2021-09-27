(ns framework.state.core
  (:require
    [xiana.core :as xiana]))

(defn make
  "Create an empty state structure."
  [deps request]
  (->
    {:deps deps
     :request  request
     :response {}}
   ;; return a state container
    xiana/map->State (conj {})))
