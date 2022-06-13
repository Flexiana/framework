(ns state-events.models.event
  (:require
    [honeysql.format :as sqlf]
    [honeysql.helpers :as sqlh]))

(defn add [state]
  (let [event       (-> state :request-data :event)
        resource    (:resource event)
        resource-id (:resource-id event)]
    (assoc-in state
              [:db-queries :queries]
              [(-> (sqlh/insert-into :events)
                   (sqlh/values [(update event :payload sqlf/value)]))
               (-> (sqlh/select :*)
                   (sqlh/from :events)
                   (sqlh/where [:and
                                [:= :resource resource]
                                [:= :resource-id resource-id]]))])))

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
  (assoc state
         :query
         (-> (sqlh/select :*)
             (sqlh/from :events))))
