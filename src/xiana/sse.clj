(ns xiana.sse
  "SSE aka Server sent event solution based on websockets."
  (:require
    [clojure.core.async :as async :refer (<! go-loop)]
    [clojure.data.json :as json]
    [ring.adapter.jetty9 :as jetty]
    [taoensso.timbre :as log])
  (:import
    (java.lang
      AutoCloseable)))

(def close-channel
  "Close a client's channel"
  jetty/close!)

(def ^:private headers {"Content-Type" "text/event-stream"})

(def ^:private EOL "\n")

(defn ^:private ->message [data]
  (str "data: " (json/write-str data) EOL EOL))

(defn- clients->channels
  [clients]
  (reduce into (vals clients)))

(defrecord ^:private closable-events-channel
  [channel clients]
  AutoCloseable
  (close [this]
    (.close! (:channel this))
    (doseq [c (clients->channels @(:clients this))]
      (jetty/close! c))))

(defn init
  "Opens the SSE input channel and injects it into the config map"
  [config]
  (let [channel (async/chan 5)
        clients (atom {})]
    (go-loop []
      (when-let [data (<! channel)]
        (log/debug "Sending data via SSE: " data)
        (doseq [c (clients->channels @clients)]
          (jetty/send! c (->message data)))
        (recur)))
    (assoc config :events-channel (->closable-events-channel
                                    channel
                                    clients))))

(defn- server-event-channel
  [state]
  (let [clients (get-in state [:deps :events-channel :clients])
        session-id (get-in state [:session-data :session-id])]
    {:on-connect (fn [ch]
                   (swap! clients update session-id (fnil conj #{}) ch)
                   (jetty/send! ch {:headers headers :body (json/write-str {})}))
     :on-text    (fn [c m] (jetty/send! c m))
     :on-close   (fn [ch _status _reason] (swap! clients update session-id disj ch))}))

(defn stop-heartbeat-loop
  "Closes input channel for sending messages"
  [state]
  (when-let [channel (get-in state [:deps :events-channel :channel])]
    (async/close! channel)))

(defn put!
  "Puts a message to SSE input channel for broadcast it to all clients"
  [state message]
  (let [events-channel (get-in state [:deps :events-channel :channel])]
    (async/put! events-channel message)))

(defn put->session
  "Puts a message directly addressed to one client, by its session id"
  [deps session-id message]
  (let [clients (get-in deps [:events-channel :clients])
        session-clients (get @clients session-id)]
    (doseq [c session-clients] (jetty/send! c (->message message)))
    (not-empty session-clients)))

(defn sse-action
  "A web-socket action to subscribe a client to SSE channel"
  [state]
  (assoc-in state [:response-data :channel] (server-event-channel state)))
