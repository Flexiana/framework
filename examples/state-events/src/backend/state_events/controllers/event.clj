(ns state-events.controllers.event
  (:require
    [clojure.walk :refer [keywordize-keys]]
    [jsonista.core :as json]
    [state-events.controller-behaviors.sse :as sse]
    [state-events.models.event :as model]
    [state-events.views.event :as view]
    [xiana.core :as xiana]
    [xiana.db :as db]))

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

(defn create-resource
  [state]
  (let [action (-> state :request-data :event :action str)
        last-event (last-event state)]
    (cond
      (not= "create" action) (invalid-action state "Action and method not matching")
      (some? last-event) (resource-exist-error state "Resource already exists")
      :default (xiana/flow-> (assoc state
                                    :view view/aggregate
                                    :side-effect sse/send-event!)
                             model/add))))

(defn ok
  [state]
  (xiana/flow-> (assoc state
                       :view view/aggregate
                       :side-effect sse/send-event!)
                model/add))

(defn modify
  [state]
  (let [{:keys [:action :creator :payload]} (-> state :request-data :event)
        last-event (last-event state)
        last-action (:events/action last-event)
        last-creator (:events/creator last-event)]
    (if (or (nil? last-event) (= "delete" last-action))
      (resource-exist-error state "Resource does not exists")
      (case action
        "dissoc-key" (let [k (:remove payload)]
                       (case k
                         "resource" (invalid-action state "Cannot delete resource type")
                         "id" (invalid-action state "Cannot delete id of resource")
                         (ok state)))
        "modify" (ok state)
        "clean" (ok state)
        "undo" (if (and (= last-creator creator)
                        (not= "create" last-action))
                 (ok state)
                 (invalid-action state "Cannot undo, resource already modified by someone else!"))
        "redo" (cond
                 (not= last-creator creator) (invalid-action state "Cannot redo, resource already modified by someone else!")
                 (not= "undo" last-action) (invalid-action state "Cannot redo, last action wasn't undo!")
                 :default (ok state))
        (invalid-action state "Action and method not matching")))))

(defn delete
  [state]
  (let [{:keys [:action :creator :payload]} (-> state :request-data :event)]
    (if (nil? last-event)
      (resource-exist-error state "Resource does not exists")
      (case action
        "delete" (ok state)
        (invalid-action state "Action and method not matching")))))

(defn persons
  [state]
  (xiana/flow-> (assoc state :view view/persons)
                model/fetch))

(defn raw
  [state]
  (xiana/flow-> (assoc state :view view/raw)
                model/fetch))
