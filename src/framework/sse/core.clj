(ns framework.sse.core
  (:require
    [clojure.core.async :as async :refer (go <! go-loop)]
    [clojure.data.json :as json]
    [clojure.tools.logging :as log]
    [org.httpkit.server :as server]
    [xiana.core :as xiana])
  (:import
    (java.lang
      AutoCloseable)
    (java.util
      Date)))

(def headers {"Content-Type" "text/event-stream"})

(def EOL "\n")

(defn ->message [data]
  (str "data: " (json/write-str data) EOL EOL))

(defrecord closable-events-channel
  [channel clients]
  AutoCloseable
  (close [this]
    (.close! (:channel this))
    (doseq [c @(:clients this)]
      (.close c))))

(defn init [config]
  (let [channel (async/chan 5)
        clients (atom #{})]
    (go-loop [i 0]
      (if-not (async/>! channel {:type :ping :id i :timestamp (.getTime (Date.))})
        (log/error "the events channel was closed!")
        (do
          (async/<! (async/timeout 10000))
          (recur (inc i)))))
    (go-loop []
      (when-let [data (<! channel)]
        (log/debug "Sending data via SSE: " data)
        (doseq [c @clients]
          (server/send! c (->message data) false))
        (recur)))
    (assoc config :events-channel (->closable-events-channel
                                    channel
                                    clients))))

(defn server-event-channel [state]
  (let [clients (get-in state [:deps :events-channel :clients])]
    (server/as-channel (:request state)
                       {:init       (fn [ch]
                                      (swap! clients conj ch)
                                      (server/send! ch {:headers headers} false))
                        :on-receive (fn [ch message])
                        :on-ping    (fn [ch data])
                        :on-close   (fn [ch status] (swap! clients disj ch))
                        :on-open    (fn [ch])})))

(defn stop-heartbeat-loop
  [state]
  (when-let [channel (get-in state [:deps :events-channel :channel])]
    (async/close! channel)))

(defn put!
  [state message]
  (let [events-channel (get-in state [:deps :events-channel :channel])]
    (async/put! events-channel message)))

(defn sse-action
  [state]
  (xiana/ok
    (assoc state
           :response
           (server-event-channel state))))
