(ns state-events.interceptors.event-process
  (:require
    [jsonista.core :as json]
    [tick.core :as t]
    [xiana.core :as xiana])
  (:import
    (java.sql
      Timestamp)))

(defn ->event
  [state]
  (let [body (-> state :request :body)
        payload (json/write-value-as-string (dissoc body :action))
        creator (-> state :session-data :users/id)
        action (:action body)
        event {:payload     payload
               :resource    (:resource body)
               :resource-id (:id body)
               :modified-at (Timestamp/from (t/now))
               :action      action
               :creator     creator}]
    (xiana/ok (assoc-in state [:request-data :event] event))))

(defn process-actions
  [events]
  (reduce (fn [acc event]
            (case (:action event)
              :undo (butlast acc)
              :delete (mapv #(assoc % :payload "{}") (conj acc event))
              (conj acc event)))
          [] events))

(defn ->aggregate
  [state]
  (let [events (->> state
                    :response-data
                    :db-data
                    (sort-by :modified-at)
                    process-actions)
        payloads (map #(json/read-value (:payload %) json/keyword-keys-object-mapper) events)
        event-aggregate (reduce merge events)
        payload-aggregate (reduce merge payloads)]
    (xiana/ok
      (assoc-in state [:response-data :event-aggregate] (assoc event-aggregate :payload payload-aggregate)))))

(def interceptor
  {:enter ->event
   :leave ->aggregate})
