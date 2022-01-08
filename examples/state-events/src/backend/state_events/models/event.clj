(ns state-events.models.event
  (:require
    [honeysql.helpers :as sqlh]
    [jsonista.core :as json]
    [xiana.core :as xiana])
  (:import
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

(defn add [state]
  (let [event (-> state :request-data :event)
        resource (:resource event)
        resource-id (:resource-id event)]
    (xiana/ok
      (assoc-in state
                [:db-queries :queries]
                [(-> (sqlh/insert-into :events)
                     (sqlh/values [event]))
                 (-> (sqlh/select :*)
                     (sqlh/from :events)
                     (sqlh/where [:and
                                  [:= :resource resource]
                                  [:= :resource-id resource-id]]))]))))

(defn last-event
  [state]
  (let [event (-> state :request-data :event)
        resource (:resource event)
        resource-id (:resource-id event)]
    (-> (sqlh/select :*)
        (sqlh/from :events)
        (sqlh/where [:and
                     [:= :events.resource resource]
                     [:= :events.resource-id resource-id]])
        (sqlh/order-by [:events/modified_at :desc])
        (sqlh/limit 1))))

(defn fetch
  [state]
  (xiana/ok
    (assoc state
           :query
           (-> (sqlh/select :*)
               (sqlh/from :events)))))
