(ns frames.controllers.status
  (:require
    [corpus.responses :as responses]
    [xiana.core :as xiana]))

(defn handle-status
  [state]
  (xiana/ok
    (assoc state :response
           (responses/ok {:status "OK"}))))
