(ns migrator.events
  (:require
    [migrator.db :as db]
    [re-frame.core :as re-frame]))

(re-frame/reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))
