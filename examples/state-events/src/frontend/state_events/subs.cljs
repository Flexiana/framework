(ns state-events.subs
  (:require
    [re-frame.core :as re-frame]))

(re-frame/reg-sub
  :selected
  :selected)

(re-frame/reg-sub
  :persons
  :persons)
