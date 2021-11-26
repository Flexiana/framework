(ns state-events.controllers.event
  (:require
    [clojure.walk :refer [keywordize-keys]]
    [framework.db.core :as db]
    [framework.sse.core :as sse]
    [jsonista.core :as json]
    [state-events.models.event :as model]
    [state-events.views.event :as view]
    [xiana.core :as xiana])
  (:import
    (java.sql
      Timestamp)))

(defn last-event
  [state]
  (let [query (model/last-event state)
        datasource (-> state :deps :db :datasource)]
    (first (db/execute datasource query))))

(defn invalid-action
  [state msg]
  (let [event (-> state :request-data :event)
        resource (:resource event)
        resource-id (:resource-id event)
        action (:action event)]
    (xiana/error (assoc state :response
                        {:status 400
                         :body   (json/write-value-as-string
                                   {:error       msg
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
    (sse/put! state (->json (assoc agg-event :type :modify))))
  (xiana/ok state))

(defn create-resource
  [state]
  (let [action (-> state :request-data :event :action str)
        last-event (last-event state)]
    (cond
      (not= "create" action) (invalid-action state "Action and method not matching")
      (some? last-event) (resource-exist-error state "Resource already exists")
      :default (xiana/flow-> (assoc state
                                    :view view/aggregate
                                    :side-effect send-event!)
                             model/add))))

(defn ok
  [state]
  (xiana/flow-> (assoc state
                       :view view/aggregate
                       :side-effect send-event!)
                model/add))

(defn modify
  [state]
  (let [{:keys [:action :creator]} (-> state :request-data :event)
        last-event (last-event state)
        last-action (:events/action last-event)
        last-creator (:events/creator last-event)]
    (if (some? last-event)
      (case action
        "modify" (ok state)
        "clean" (ok state)
        "undo" (if (= last-creator creator)
                 (ok state)
                 (invalid-action state "Cannot undo, resource already modified by someone else!"))
        "redo" (cond
                 (not= last-creator creator) (invalid-action state "Cannot redo, resource already modified by someone else!")
                 (= last-action action) (invalid-action state "Cannot redo, last action wasn't undo!")
                 :default (ok state))
        (invalid-action state "Action and method not matching"))
      (resource-exist-error state "Resource does not exists"))))

(defn collect
  [state]
  (xiana/flow-> (assoc state :view view/fetch-all)
                model/fetch))
