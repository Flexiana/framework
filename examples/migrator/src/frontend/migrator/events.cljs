(ns migrator.events
  (:require
   [re-frame.core :as re-frame]
   [migrator.db :as db]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))
