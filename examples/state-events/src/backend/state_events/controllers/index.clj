(ns state-events.controllers.index
  (:require
    [ring.util.response :as ring]
    [xiana.core :as xiana]))

(defn handle-index
  [state]
  (xiana/ok
    (assoc state
           :response
           (ring/response "Index page"))))
