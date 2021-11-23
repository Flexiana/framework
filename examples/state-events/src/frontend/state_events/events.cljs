(ns state-events.events
  (:require
   [re-frame.core :as re-frame]
   [state-events.db :as db]))


(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))
