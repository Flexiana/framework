(ns state-events.subs
  (:require
    [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::name
  (fn [db]
    (:name db)))

(re-frame/reg-sub
  :selected
  (fn [db]
    (:selected db)))

(re-frame/reg-sub
  :persons
  :persons)
