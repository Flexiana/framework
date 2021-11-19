(ns state-events.models.event
  (:require
    [honeysql.helpers :as sql]
    [xiana.core :as xiana]))

(defn add [state]
  (let [event (-> state :request-data :event)
        resource (:resource event)
        resource-id (:resource-id event)]
    (xiana/ok
      (assoc-in state
                [:db-queries :queries]
                [(-> (sql/insert-into :events)
                     (sql/values [event]))
                 (-> (sql/select :*)
                     (sql/from :events)
                     (sql/where [:and
                                 [:= :resource resource]
                                 [:= :resource-id resource-id]]))]))))
