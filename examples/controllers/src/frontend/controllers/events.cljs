(ns controllers.events
  (:require
   [re-frame.core :as re-frame]
   [controllers.db :as db]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))
