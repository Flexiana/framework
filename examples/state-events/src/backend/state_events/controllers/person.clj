(ns state-events.controllers.person
  (:require
    [framework.sse.core :as sse]
    [state-events.models.person :as model]
    [xiana.core :as xiana]))

(defn put-response-to-sse
  [state]
  (let [response (-> state :response :body)]
    (sse/put! state response)))

(defn add
  [state]
  (xiana/flow->
    (assoc state :side-effect put-response-to-sse)
    model/add))

(defn modify
  [state]
  (xiana/flow->
    (assoc state :side-effect put-response-to-sse)
    model/modify))

(def sse)
