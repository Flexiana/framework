(ns xiana.websockets
  (:require
    [clojure.data.json :refer [read-str]]
    [clojure.edn :as edn]
    [clojure.string :as str]
    [jsonista.core :as j]
    [reitit.core :as r]
    [ring.adapter.jetty9 :as jetty]
    [taoensso.timbre :as log]
    [xiana.interceptor.queue :as queue]))

(def send! jetty/send!)
(def close! jetty/close!)

(defn string->
  "String to 'uri', uses the first word as action key"
  [s]
  (first (str/split s #"\s")))

(defn edn->
  "EDN to 'uri', converts edn string to map, extract :action key"
  [e]
  (:action (edn/read-string e)))

(defn json->
  "JSON to 'uri', converts json string to map, extract :action key"
  [j]
  (:action (j/read-value j j/keyword-keys-object-mapper)))

(defn probe->
  [e]
  (name
    (or (try (json-> e)
             (catch Exception _ nil))
        (try (edn-> e)
             (catch Exception _ nil))
        (try (string-> e)
             (catch Exception _ nil)))))

(defn router
  "Router for webSockets.
  Parameters:
    routes: reitit routes
    msg->uri: function makes routing base from message. If missing tries to solve message as json, edn and string
    state: xiana state record"
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
