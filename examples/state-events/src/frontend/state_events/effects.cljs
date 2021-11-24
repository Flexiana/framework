(ns state-events.effects
  (:require
    [ajax.core :refer [GET POST PUT]]
    [re-frame.core :as re-frame]))

(defn modify
  [event]
  (prn event))

(re-frame/reg-event-fx
  :persons/create
  (fn [_ event]
    (let [[_ v] event]
      (PUT "/person" {:params {:action   :create
                               :resource :persons
                               :id       (str v)}}))))

(defn modify-person
  [{:keys [db]} event]
  (POST "/person" {:params (merge {:action   :modify
                                   :resource :persons
                                   :id       (get-in db :selected :id)}
                                  event)}))

(re-frame/reg-event-fx
  :persons/first-name
  modify-person)

(re-frame/reg-event-fx
  :persons/last-name
  modify-person)

(re-frame/reg-event-fx
  :persons/e-mail
  modify-person)

(re-frame/reg-event-fx
  :persons/phone
  modify-person)

(re-frame/reg-event-fx
  :persons/city
  modify-person)
