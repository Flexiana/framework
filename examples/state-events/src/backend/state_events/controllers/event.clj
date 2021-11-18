(ns state-events.controllers.event
  (:require
    [framework.sse.core :as sse]
    [state-events.models.event :as model]
    [xiana.core :as xiana]))

(defn put-response-to-sse
  [state]
  (let [response (-> state :response :body)]
    (sse/put! state response)
    (xiana/ok state)))

(defn add
  [state]
  (xiana/flow->
    (assoc state :side-effect put-response-to-sse)
    model/add))