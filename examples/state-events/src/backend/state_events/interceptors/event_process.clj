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
  (let [params (if (empty? (-> state :request :params))
                 (-> state :request :body-params)
                 (-> state :request :params))
        payload (->pgobject (dissoc params :action))
        creator (-> state :session-data :users/id)
        _ (prn "params" params)
        event {:payload     payload
               :resource    (str (:resource params))
               :resource-id (UUID/fromString (:id params))
               :modified-at (Timestamp/from (t/now))
               :action      (str (:action params))
               :creator     creator}]
    (xiana/ok (assoc-in state [:request-data :event] event))))

(defn process-actions
  "Process actions for `:delete` `:undo` `:redo`"
  [events]
  (let [do-redo (reduce (fn [acc event]
                          (if (and (= ":redo" (:events/action event))
                                   (= ":undo" (:events/action (last acc))))
                            (butlast acc)
                            (conj acc event)))
                        [] events)
        do-undo (reduce (fn [acc event]
                          (if (= ":undo" (:events/action event))
                            (conj (butlast acc) event)
                            (conj acc event)))
                        [] do-redo)]
    (reduce (fn [acc event]
              (if (= ":delete" (:events/action event))
                (mapv #(dissoc % :events/payload) (conj acc event))
                (conj acc event)))
            [] do-undo)))

(defn ->aggregate
  "Aggregates events and payloads into a resource"
  [state]
  (let [events (->> state
                    :response-data
                    :db-data
                    second
                    (sort-by :modified-at)
                    process-actions)
        payloads (map #(or (some-> % :events/payload <-pgobject) {}) events)
        event-aggregate (reduce merge events)
        payload-aggregate (reduce merge payloads)]
    (xiana/ok
      (assoc-in state [:response-data :event-aggregate] (assoc event-aggregate :events/payload payload-aggregate)))))

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
