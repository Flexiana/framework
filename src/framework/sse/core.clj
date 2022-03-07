(ns framework.sse.core
  (:require
    [clojure.core.async :as async :refer (<! go-loop)]
    [clojure.data.json :as json]
    [ring.adapter.jetty9 :as jetty]
    [taoensso.timbre :as log]
    [xiana.core :as xiana])
  (:import
    (java.lang
      AutoCloseable)))

(def headers {"Content-Type" "text/event-stream"})

(def EOL "\n")

(defn ->message [data]
  (str "data: " (json/write-str data) EOL EOL))

(defn- clients->channels
  [clients]
  (reduce into (vals clients)))

(defrecord closable-events-channel
  [channel clients]
  AutoCloseable
  (close [this]
    (.close! (:channel this))
    (doseq [c (clients->channels @(:clients this))]
      (jetty/close! c))))

(defn init [config]
  (let [channel (async/chan 5)
        clients (atom {})]
    (go-loop []
      (when-let [data (<! channel)]
        (log/debug "Sending data via SSE: " data)
        (doseq [c (clients->channels @clients)]
          (jetty/send! c (->message data) false))
        (recur)))
    (assoc config :events-channel (->closable-events-channel
                                    channel
                                    clients))))

(defn- as-set
  [s v]
  (if s (conj s v) #{v}))

(defn server-event-channel [state]
  (let [clients (get-in state [:deps :events-channel :clients])
        session-id (get-in state [:session-data :session-id])]
    {:on-connect (fn [ch]
                   (swap! clients update session-id as-set ch)
                   (jetty/send! ch {:headers headers :body (json/write-str {})} false))
     :on-close   (fn [ch _status _reason] (swap! clients update session-id disj ch))}))

(defn stop-heartbeat-loop
  [state]
  (when-let [channel (get-in state [:deps :events-channel :channel])]
    (async/close! channel)))

(defn put!
  [state message]
  (let [events-channel (get-in state [:deps :events-channel :channel])]
    (async/put! events-channel message)))

(defn put->session
  [deps session-id message]
  (let [clients (get-in deps [:events-channel :clients])
        session-clients (get @clients session-id)]
    (doseq [c session-clients] (jetty/send! c (->message message) false))
    (not-empty session-clients)))

(defn sse-action
  [state]
  (xiana/ok
    (assoc state
           :response
           (jetty/ws-upgrade-response (server-event-channel state)))))
