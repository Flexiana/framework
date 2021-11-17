(ns state-events.interceptors.event-process
  (:require
    [jsonista.core :as json]
    [tick.core :as t]
    [xiana.core :as xiana]))

(defn ->event
  [state]
  (let [body (-> state :request :body)
        payload (json/write-value-as-string body)
        creator (-> state :session-data :users/id)
        action (case (-> state :request :method)
                 :put :create
                 :post :modify
                 :delete :undo)
        event {:payload     payload
               :resource    (:resource body)
               :resource-id (:id body)
               :modified_at (t/now)
               :action      action
               :creator     creator}]
    (xiana/ok (assoc-in state [:request-data :event] event))))

(defn undo-process
  [events]
  (reduce (fn [acc event]
            (case (:action event)
              :undo (butlast acc)
              (conj acc event)))
          [] events))

(defn ->aggregate
  [state]
  (let [events (->> state
                    :response-data
                    :db-data
                    (sort-by :events/created-at)
                    undo-process)
        payloads (map #(json/read-value (:payload %) json/keyword-keys-object-mapper) events)
        event-aggregate (apply merge events)
        payload-aggregate (apply merge payloads)]
    (xiana/ok
      (assoc-in state [:response-data :event-aggregate] (assoc event-aggregate :payload payload-aggregate)))))

(def interceptor
  {:enter ->event
   :leave ->aggregate})
