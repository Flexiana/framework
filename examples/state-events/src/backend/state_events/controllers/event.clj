(ns state-events.controllers.event
  (:require
    [framework.sse.core :as sse]
    [jsonista.core :as json]
    [state-events.models.event :as model]
    [state-events.views.event :as view]
    [xiana.core :as xiana]))

(defn add
  [state]
  (xiana/flow-> (assoc state :view view/view)
                model/add))
