(ns state-events.models.event)

(defn add [state]
  (let [event (-> state :request-data :event)
        resource (:resource event)
        resource-id (:resource-id event)]
    (assoc-in state
              [:db-queries :queries]
              [{:insert-into :events
                :values      event}
               {:select :*
                :from :events
                :where [:and
                        [:= :resource resource]
                        [:= :resource-id resource-id]]}])))

