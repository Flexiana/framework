(ns state-events.models.event
  (:require
    [honeysql.core :as sql]
    [honeysql.helpers :as sqlh]
    [xiana.core :as xiana]))

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

(defn exists
  [state]
  (let [event (-> state :request-data :event)
        resource (:resource event)
        resource-id (:resource-id event)]
    (-> (sqlh/select (sql/call :count :*))
        (sqlh/from :events)
        (sqlh/where [:and
                     [:= :events.resource resource]
                     [:= :events.resource-id resource-id]]))))

(defn fetch
  [state]
  (xiana/ok
    (assoc state
           :query
           (-> (sqlh/select :*)
               (sqlh/from :events)))))
