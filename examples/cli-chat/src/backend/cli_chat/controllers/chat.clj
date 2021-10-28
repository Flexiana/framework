(ns cli-chat.controllers.chat
  (:require
    [cli-chat.controller-behaviors.chat :as behave]
    [clojure.pprint]
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [framework.handler.core]
    [reitit.core :as r]
    [xiana.core :as xiana]))

(defonce channels (atom {}))

(def routes
  (r/router [["/help" {:action behave/help}]
             ["/login" {:action behave/login}]]))

(defn router
  [routes
   {:keys [income-msg fallback] :as state}]
  (log/info "Processing: " (str/trim income-msg))
  (when-not (str/blank? income-msg)
    (let [match (r/match-by-path routes (str/trim (first (str/split income-msg #" "))))
          action (get-in match [:data :action] fallback)]
      (action state))))

(def routing
  (partial router routes))

(defn chat-action
  [state]
  (xiana/ok
    (assoc-in state [:response-data :channel]
              {:on-receive (fn [ch msg]
                             (routing (assoc state :ch ch :income-msg msg :fallback behave/broadcast :channels channels)))
               :on-open    (fn [ch]
                             (behave/welcome (assoc state :ch ch)))
               :on-ping    (fn [ch data])
               :on-close   (fn [ch status] (swap! channels dissoc ch))
               :init       (fn [ch])})))
