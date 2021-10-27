(ns cli-chat.events
  (:require
   [re-frame.core :as re-frame]
   [cli-chat.db :as db]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))
