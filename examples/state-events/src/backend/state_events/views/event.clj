(ns state-events.views.event
  (:require
    [clojure.walk :refer [keywordize-keys]]
    [state-events.interceptors.event-process :refer [event->agg]]
    [xiana.core :as xiana]))

(def aggregate
  (fn [state]
    (xiana/ok
      (assoc-in state
                [:response :body :data]
                (-> state :response-data :event-aggregate)))))

(defn group-events
  [state]
  (->> state :response-data :db-data
       (group-by #(format "%s/%s" (apply str (rest (:events/resource %))) (:events/resource_id %)))
       keywordize-keys))

(defn persons
  [state]
  (xiana/ok
    (assoc-in state [:response :body :data]
              (reduce (fn [acc [k v]] (into acc {k (:events/payload (event->agg v))}))
                      {} (group-events state)))))

(defn raw
  [state]
  (xiana/ok
    (assoc-in state [:response :body :data]
              (group-events state))))
