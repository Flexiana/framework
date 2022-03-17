(ns state-events.controller-behaviors.sse
  (:require
    [clojure.core.async :as async]
    [framework.sse.core :as sse])
  (:import
    (java.sql
      Timestamp)
    (java.util
      Date)))

(defn prepare->json
  [m]
  (reduce
    (fn [acc [k v]]
      (into acc {k (cond (uuid? v) (str v)
                         (instance? Timestamp v) (.getTime v)
                         :else v)})) {} m))

(defn send-event!
  [state]
  (let [agg-event (get-in state [:response-data :event-aggregate])]
    (sse/put! state (prepare->json (assoc agg-event :type :modify))))
  state)

(defonce ^{:private true} ping-id (atom 0))

(defn ping [deps]
  (let [channel (get-in deps [:events-channel :channel])]
    (async/>!! channel {:type :ping :id (swap! ping-id inc) :timestamp (.getTime (Date.))})))
