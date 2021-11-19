(ns state-events.views.event
  (:require
    [xiana.core :as xiana]))

(def view
  (fn [state]
    (xiana/ok
      (assoc-in state
                [:response :body :data]
                (-> state :response-data :event-aggregate)))))
