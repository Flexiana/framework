(ns state-events.web-sockets
  (:require
    [clojure.string :as str]
    [clojure.walk :refer [keywordize-keys]]
    [re-frame.core :as re-frame]))

(defonce channel (atom nil))

(defn normalize-sse-message [m]
  (-> m
      keywordize-keys
      (update :type keyword)))

(defn handle-response! [response]
  (let [data (-> (.parse js/JSON (str/replace-first (.-data response) #"data: " ""))
                 (js->clj :keywordize-keys true)
                 (update :type keyword))]
    (prn "data " data)
    (case (:type data)
      :ping (prn "ping")
      :modify (let [k (keyword (apply str (rest (:resource data)))
                               (:resource_id data))]
                (re-frame/dispatch [:persons/modify k data]))
      (constantly nil))))

(defn connect! [url]
  (if-let [chan (js/WebSocket. url)]
    (do
      (.log js/console "Connected!")
      (set! (.-onmessage chan) handle-response!)
      (reset! channel chan))
    (throw (ex-info "Websocket Connection Failed!"
                    {:url url}))))
