(ns cli-chat.controllers.chat
  (:require [xiana.core :as xiana]
            [clojure.pprint]
            [framework.session.core :as session]
            [org.httpkit.server :as server]
            [clojure.string :as str]
            [shadow.jvm-log :as log]))

(def welcome-message
  "HELLo tHERE.
  type '/sign-up' to register
  '/login username' to log in,
   get help with '/help'")

(defonce channels (atom #{}))

(defn send-multi-line
  [ch msg]
  (doseq [m (str/split-lines msg)]
    (server/send! ch m false)))

(defn broadcast
  [ch msg]
  (doseq [c (remove #{ch} @channels)]
    (send-multi-line c msg)))



(defn router
  [ch msg]
  (clojure.pprint/pprint msg)
  (case (str/trim msg)
    "/help" (server/send! ch "Help message!")
    (broadcast ch msg)))

(defn chat-action
  [state]
  (xiana/ok
    (assoc-in state [:response-data :channel]
              {:on-receive router
               :on-open    (fn [ch]
                             (swap! channels conj ch)
                             (send-multi-line ch welcome-message))
               :on-ping    (fn [ch data])
               :on-close   (fn [ch status] (swap! channels disj ch))
               :init       (fn [ch])})))
