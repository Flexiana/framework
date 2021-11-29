(ns state-events.effects
  (:require
    [ajax.core :refer [GET POST PUT]]
    [clojure.walk :refer [keywordize-keys]]
    [re-frame.core :as re-frame]))

(defn on-error
  [response]
  (js/alert (str (->> response :response
                      (.parse js/JSON)
                      js->clj
                      keywordize-keys
                      :error))))

(re-frame/reg-event-fx
  :persons/modify
  (fn [cofx event]
    (let [[_ k v] event
          payload (assoc (:payload v)
                         :--last-action (:action v)
                         :--last-modified-by (:creator v))
          db (:db cofx)]
      {:db (cond->
             (assoc-in db [:persons k] payload)
             (= (name k)
                (get-in db [:selected :id]))
             (assoc :selected payload))})))

(re-frame/reg-event-fx
  :persons/create
  (fn [{:keys [db]} event]
    (let [[_ v] event]
      (PUT "/person" {:params        {:action   :create
                                      :resource :persons
                                      :id       (str v)}
                      :error-handler on-error}))
    {:db db}))

(defn modify-person
  [{:keys [db]} event]
  (POST "/person" {:params        (merge {:action   :modify
                                          :resource :persons
                                          :id       (get-in db [:selected :id])}
                                         event)
                   :error-handler on-error})
  {:db db})

(re-frame/reg-event-fx
  :selected/clean
  (fn [{:keys [db]} event]
    (let [[_ v] event]
      (POST "/person" {:params        (merge (:selected db)
                                             {:action   :clean
                                              :resource :persons})
                       :error-handler on-error}))
    {:db db}))

(re-frame/reg-event-fx
  :selected/undo
  (fn [{:keys [db]} event]
    (let [[_ v] event]
      (POST "/person" {:params        (merge (:selected db)
                                             {:action   :undo
                                              :resource :persons})
                       :error-handler on-error}))
    {:db db}))

(re-frame/reg-event-fx
  :selected/redo
  (fn [{:keys [db]} event]
    (let [[_ v] event]
      (POST "/person" {:params        (merge (:selected db)
                                             {:action   :redo
                                              :resource :persons})
                       :error-handler on-error}))
    {:db db}))

(re-frame/reg-event-fx
  :field/remove
  (fn
    [{:keys [db]} event]
    (POST "/person" {:params        (merge {:action   :dissoc-key
                                            :resource :persons
                                            :id       (get-in db [:selected :id])}
                                           event)
                     :error-handler on-error})
    {:db db}))

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

(re-frame/reg-event-db
  :table/click
  (fn [db event]
    (prn "event" event)
    (let [id (second event)]
      (prn "ID" (name id))
      (prn (get-in db [:persons id]))
      (if (= (name id) (get-in db [:selected :id]))
        (assoc db :selected {})
        (assoc db :selected (get-in db [:persons id]))))))
