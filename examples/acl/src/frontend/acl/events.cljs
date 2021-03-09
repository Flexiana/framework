(ns acl.events
  (:require
   [re-frame.core :as re-frame]
   [acl.db :as db]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))
