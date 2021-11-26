(ns state-events.interceptors.event-process
  (:require
    [clojure.tools.logging :as logging]
    [jsonista.core :as json]
    [tick.core :as t]
    [xiana.core :as xiana])
  (:import
    (java.sql
      Timestamp)
    (java.util
      UUID)
    (org.postgresql.util
      PGobject)))

(def mapper (json/object-mapper {:decode-key-fn keyword}))
(def ->json json/write-value-as-string)
(def <-json #(json/read-value % mapper))

(defn ->pgobject
  "Transforms Clojure data to a PGobject that contains the data as
  JSON. PGObject type defaults to `jsonb` but can be changed via
  metadata key `:pgtype`"
  [x]
  (let [pgtype (or (:pgtype (meta x)) "jsonb")]
    (doto (PGobject.)
      (.setType pgtype)
      (.setValue (->json x)))))

(defn <-pgobject
  "Transform PGobject containing `json` or `jsonb` value to Clojure
  data."
  [^PGobject v]
  (let [type (.getType v)
        value (.getValue v)]
    (if (#{"jsonb" "json"} type)
      (when value
        (with-meta (<-json value) {:pgtype type}))
      value)))

(defn ->event
  "Inject event int the state"
  [state]
  (let [params (reduce (fn [acc [k v]] (into acc {(keyword (name k)) v}))
                       {} (if (empty? (-> state :request :params))
                            (-> state :request :body-params)
                            (-> state :request :params)))
        action (name (:action params))
        p (cond-> (dissoc params :action)
            (some? (#{"undo" "redo" "clean"} action)) (select-keys [:id :resource]))
        payload (->pgobject p)
        creator (-> state :session-data :users/id)
        event {:payload     payload
               :resource    (name (:resource params))
               :resource-id (UUID/fromString (:id params))
               :modified-at (Timestamp/from (t/now))
               :action      action
               :creator     creator}]
    (xiana/ok (assoc-in state [:request-data :event] event))))

(defn clean-pg
  [^PGobject obj]
  (-> obj
      <-pgobject
      (select-keys [:id :resource])
      ->pgobject))

(defn process-actions
  "Process actions for `clean` `undo` `redo`"
  [events]
  (let [do-redo (reduce (fn [acc event]
                          (if (and (= "redo" (:events/action event))
                                   (= "undo" (:events/action (last acc))))
                            (vec (butlast acc))
                            (conj acc event)))
                        [] events)
        do-undo (reduce (fn [acc event]
                          (if (and (= "undo" (:events/action event))
                                   (not= "create" (:events/action (last acc))))
                            (vec (butlast acc))
                            (conj acc event)))
                        [] do-redo)]
    (reduce (fn [acc event]
              (if (= "clean" (:events/action event))
                (mapv #(update % :events/payload clean-pg) (conj acc event))
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
