(ns state-events.web-sockets
  (:require [re-frame.core :as re-frame]
            [cljs.reader :as edn]))

(defonce channel (atom nil))

(defn handle-response! [response]
  (if-let [errors (:errors response)]
    (re-frame/dispatch [:form/set-server-errors errors])
    (do
      (re-frame/dispatch [:message/add response])
      (re-frame/dispatch [:form/clear-fields response]))))

(defn connect! [url receive-handler]
  (if-let [chan (js/WebSocket. url)]
    (do
      (.log js/console "Connected!")
      (set! (.-onmessage chan) #(->> %
                                     .-data
                                     edn/read-string
                                     receive-handler))
      (reset! channel chan))
    (throw (ex-info "Websocket Connection Failed!"
                    {:url url}))))