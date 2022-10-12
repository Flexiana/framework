(ns xiana.websockets
  (:require
    [clojure.string :as str]
    [reitit.core :as r]
    [ring.adapter.jetty9 :as jetty]
    [taoensso.timbre :as log]
    [xiana.interceptor.queue :as queue]
    [xiana.websockets.router-helpers :refer [probe->]]))

(def send!
  "Sending a message on websockets"
  jetty/send!)

(def close!
  "Closes a websockets channel"
  jetty/close!)

(defn router
  "Router for webSockets.
  Parameters:
    routes: reitit routes
    msg->uri: function makes routing base from message. If missing tries to solve message as json, edn and string
    state: xiana state record

  Example:
  ```clojure
  (def routing (partial router routes string->))
  ```"
  ([routes state]
   (router routes probe-> state))
  ([routes msg->uri {{income-msg :income-msg
                      fallback   :fallback} :request-data
                     :as                    state}]
   (when-not (str/blank? income-msg)
     (let [match (r/match-by-path routes (msg->uri income-msg))
           action (get-in match [:data :action] fallback)
           interceptors (get-in match [:data :interceptors])
           default-interceptors (get-in match [:data :default-interceptors])
           _ (or (get-in match [:data :hide])
                 (log/info "Processing: " (str/trim income-msg)))
           update-state (-> state
                            (update :request-data assoc
                                    :action action
                                    :interceptors interceptors)
                            (queue/execute default-interceptors))]
       (when-let [reply-fn (get-in update-state [:response-data :reply-fn])]
         (reply-fn update-state))))))

(defn ws-action
  "Injects ws-handler as response channel.
  For ws-handler, follow: [ring-adapter's doc-string](https://github.com/sunng87/ring-jetty9-adapter/blob/master/src/ring/adapter/jetty9/websocket.clj#L235)"
  [ws-handler]
  (fn [state]
    (assoc-in state [:response-data :channel]
              ws-handler)))
