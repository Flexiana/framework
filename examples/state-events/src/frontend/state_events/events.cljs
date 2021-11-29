(ns state-events.events
  (:require
    [ajax.core :refer [GET POST PUT]]
    [clojure.walk :refer [keywordize-keys]]
    [re-frame.core :as re-frame]
    [state-events.db :as db]))

(re-frame/reg-event-db
  ::initialize-db
  (fn [_ _]
    (GET "/person"
         {:handler #(re-frame/dispatch [::update-db %])})
    db/default-db))

(re-frame/reg-event-db
  ::update-db
  (fn [db [_ v]]
    (let [value (keywordize-keys v)]
      (prn "Hello v" value)
      (assoc db :persons (:data value)))))
