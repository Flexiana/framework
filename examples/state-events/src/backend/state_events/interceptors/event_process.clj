(ns state-events.interceptors.event-process
  (:require
    [clojure.tools.logging :as logging]
    [state-events.models.event :refer [<-pgobject ->pgobject]]
    [tick.core :as t]
    [xiana.core :as xiana])
  (:import
    (java.sql
      Timestamp)
    (java.util
      UUID)
    (org.postgresql.util
      PGobject)))

(defn- clean-pg
  [^PGobject obj]
  (-> obj
      <-pgobject
      (select-keys [:id :resource])
      ->pgobject))

(defn- rem-k
  [^PGobject obj k]
  (-> obj
      <-pgobject
      (dissoc (keyword k))
      ->pgobject))

(defn- remove-key
  [acc event]
  (let [rm-k (:remove (<-pgobject (:events/payload event)))]
    (mapv #(update % :events/payload rem-k rm-k) acc)))

(defn ->event
  "Inject event int the state"
  [state]
  (let [params (reduce (fn [acc [k v]] (into acc {(keyword (name k)) v}))
                       {} (if (empty? (-> state :request :params))
                            (-> state :request :body-params)
                            (-> state :request :params)))
        action (name (:action params))
        p (cond-> (dissoc params :action)
            (some? (#{"undo" "redo" "clean" "delete"} action)) (select-keys [:id :resource])
            (= "dissoc-key" action) (select-keys [:id :resource :remove]))
        payload (->pgobject p)
        creator (-> state :session-data :users/id)
        event {:payload     payload
               :resource    (name (:resource params))
               :resource-id (UUID/fromString (:id params))
               :modified-at (Timestamp/from (t/now))
               :action      action
               :creator     creator}]
    (xiana/ok (assoc-in state [:request-data :event] event))))

(defn process-actions
  "Process actions for `clean` `undo` `redo` `dissoc-key`"
  [events]
  (let [do-redo (reduce (fn [acc event]
                          (if (and (= "redo" (:events/action event))
                                   (= "undo" (:events/action (last acc))))
                            (vec (butlast acc))             ; TODO undo for same user?
                            (conj acc event)))
                        [] events)
        do-undo (reduce (fn [acc event]
                          (if (and (= "undo" (:events/action event))
                                   (not= "create" (:events/action (last acc))))
                            (vec (butlast acc))             ; TODO undo for same user?
                            (conj acc event)))
                        [] do-redo)]
    (reduce (fn [acc event]
              (case (:events/action event)
                "clean" (mapv #(update % :events/payload clean-pg) (conj acc event))
                "dissoc-key" (remove-key acc event)
                (conj acc event)))
            [] do-undo)))

(defn event->agg
  [events]
  (let [sorted (sort-by :events/modified_at events)
        actions (process-actions sorted)
        payloads (map #(or (some-> % :events/payload <-pgobject) {}) actions)
        payload-aggregate (reduce merge payloads)]
    (assoc (last sorted) :events/payload payload-aggregate)))

(defn ->aggregate
  "Aggregates events and payloads into a resource"
  [state]
  (let [events (-> state
                   :response-data
                   :db-data
                   second)]
    (xiana/ok
      (assoc-in state [:response-data :event-aggregate] (event->agg events)))))

(def interceptor
  "Event processing interceptor
  :enter injects request parameters as events
  :leave aggregates events from database into a resource"
  {:name  :event-process
   :enter ->event
   :leave ->aggregate
   :error (fn [state]
            (let [e (:exception state)]
              (logging/error "Got exception: " e)
              (throw e)))})
