(ns xiana.state)

;; state/context record definition
(defrecord State
    [request request-data response session-data deps])

(defn make
  "Create an empty state structure."
  [deps request]
  (->
    {:deps deps
     :request  request
     :response {}}
    map->State (conj {})))
