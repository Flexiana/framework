(ns framework.state.core
  (:require
   [xiana.core :as xiana]))

(defn make
  "Create an empty state structure."
  [request]
  (->
   {:deps {:auth nil}
    :request  request
    :response {}}
   ;; return a state container
   xiana/map->State (conj {})))
