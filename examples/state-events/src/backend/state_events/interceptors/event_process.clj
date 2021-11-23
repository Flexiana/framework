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
  [state]
  (let [params (-> state :request :params)
        payload (->pgobject (dissoc params :action))
        creator (-> state :session-data :users/id)
        event {:payload     payload
               :resource    (:resource params)
               :resource-id (UUID/fromString (:id params))
               :modified-at (Timestamp/from (t/now))
               :action      (:action params)
               :creator     creator}]
    (xiana/ok (assoc-in state [:request-data :event] event))))

(defn process-actions
  [events]
  (let [do-redo (reduce (fn [acc event]
                          (if (and (= :redo (:events/action event))
                                   (= :undo (:events/action (last acc))))
                            (butlast acc)
                            (conj acc event)))
                        []
                        events)
        do-undo (reduce (fn [acc event]
                          (if (= :undo (:events/action event))
                            (conj (butlast acc) event)
                            (conj acc event)))
                        []
                        do-redo)]
    (reduce (fn [acc event]
              (if (= :delete (:events/action event))
                (mapv #(dissoc % :events/payload) (conj acc event))
                (conj acc event)))
            [] do-undo)))

(defn ->aggregate
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
  {:name  :event-process
   :enter ->event
   :leave ->aggregate
   :error (fn [state]
            (let [e (:exception state)]
              (logging/error "Got exception: " e)
              (throw e)))})
