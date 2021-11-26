(ns state-events.controllers.event
  (:require
    [clojure.tools.logging :as logger]
    [clojure.walk :refer [keywordize-keys]]
    [framework.db.core :as db]
    [framework.sse.core :as sse]
    [honeysql.core :as sql]
    [jsonista.core :as json]
    [state-events.models.event :as model]
    [state-events.views.event :as view]
    [xiana.core :as xiana])
  (:import
    (java.sql
      Timestamp)))

(defn exists
  [state]
  (let [query (model/exists state)
        datasource (-> state :deps :db :datasource)
        c (-> (db/execute datasource query)
              first
              :count)]
    (not= 0 c)))

(defn invalid-action
  [state]
  (let [event (-> state :request-data :event)
        resource (:resource event)
        resource-id (:resource-id event)
        action (:action event)]
    (xiana/error (assoc state :response
                        {:status 400
                         :body   (json/write-value-as-string
                                   {:error       "Action and method not matching"
                                    :resource    resource
                                    :resource-id resource-id
                                    :action      action})}))))

(defn resource-exist-error
  [state message]
  (let [event (-> state :request-data :event)
        resource (:resource event)
        resource-id (:resource-id event)]
    (xiana/error (assoc state :response
                        {:status 403
                         :body   (json/write-value-as-string
                                   {:error       message
                                    :resource    resource
                                    :resource-id resource-id})}))))

(defn ->json
  [m]
  (reduce
    (fn [acc [k v]]
      (into acc {k (cond (uuid? v) (str v)
                         (instance? Timestamp v) (.getTime v)
                         :else v)})) {} m))

(defn send-event!
  [state]
  (let [agg-event (get-in state [:response-data :event-aggregate])]
    (prn "EVENT" agg-event)
    (sse/put! state (->json (assoc agg-event :type :modify))))
  (xiana/ok state))

(defn add
  [state]
  (let [action (-> state :request-data :event :action str)]
    (cond
      (not= "create" action) (invalid-action state)
      (exists state) (resource-exist-error state "Resource already exists")
      :default (xiana/flow-> (assoc state
                                    :view view/view
                                    :side-effect send-event!)
                             model/add))))

(defn modify
  [state]
  (let [action (-> state :request-data :event :action)]
    (cond
      (nil? (#{"modify" "undo" "redo" "clean"} action)) (invalid-action state)
      (exists state) (xiana/flow-> (assoc state
                                          :view view/view
                                          :side-effect send-event!)
                                   model/add)
      :default (resource-exist-error state "Resource does not exists"))))

(defn collect
  [state]
  (xiana/flow-> (assoc state :view view/fetch-all)
                model/fetch))
