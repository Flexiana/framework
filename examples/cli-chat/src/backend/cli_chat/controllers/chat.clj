(ns cli-chat.controllers.chat
  (:require
    [clojure.pprint]
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [org.httpkit.server :as server]
    [reitit.core :as r]
    [xiana.core :as xiana]))

(def welcome-message
  "HELLo tHERE.
  type '/sign-up' to register
  '/login username' to log in,
   get help with '/help'")

(defonce channels (atom {}))

(defn send-multi-line
  [ch msg]
  (doseq [m (str/split-lines msg)]
    (server/send! ch m false)))

(defn broadcast
  ([ch msg]
   (doseq [c (remove #(#{ch} (key %)) @channels)]
     (send-multi-line (key c) msg)))
  ([{:keys [ch msg]}]
   (broadcast ch msg)))

(def routes
  (r/router [["/help" {:action (fn [{:keys [ch msg]}] (server/send! ch "Help message!"))}]
             ["/login" {:action (fn [{:keys [ch msg]}] (server/send! ch "init login seq!"))}]]))

(defn router
  [state routes default ch msg]
  (log/info "Processing: " msg)
  (let [match (r/match-by-path routes (str/trim (first (str/split msg #" "))))
        action (get-in match [:data :action] default)]
    (action (assoc state :ch ch :msg msg))))

(defn on-receive
  [state]
  (partial router state routes broadcast))

(defn chat-action
  [state]
  (xiana/ok
    (assoc-in state [:response-data :channel]
              {:on-receive (on-receive state)
               :on-open    (fn [ch]
                             (swap! channels assoc ch (:session-data state))
                             (send-multi-line ch welcome-message))
               :on-ping    (fn [ch data])
               :on-close   (fn [ch status] (swap! channels dissoc ch))
               :init       (fn [ch])})))
