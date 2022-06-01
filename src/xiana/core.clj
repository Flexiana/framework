(ns xiana.core)

;; state/context record definition
(defrecord State
  [request request-data response session-data deps])
