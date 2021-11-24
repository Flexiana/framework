(ns state-events.effects
  (:require
    [ajax.core :refer [GET POST PUT]]
    [re-frame.core :as re-frame]))

(re-frame/reg-event-db
  :persons/modify
  (fn [db event]
    (let [[k v] event]
      ())))

(re-frame/reg-event-db
  :persons/create
  (fn [db event]
    (let [[k v] event]
      (prn db)
      (prn k)
      (prn v)
      (PUT "/person" {:params {:action :create
                               :resource :persons
                               :id (str v)}}))))

(re-frame/reg-event-fx
  :persons/first-name
  (fn [db event]
    (prn "DB " db)
    (prn "Event " event)))
