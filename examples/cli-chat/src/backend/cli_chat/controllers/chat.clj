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
             ["/welcome" {:action behave/welcome}]
             ["/me" {:action behave/me}]
             ["/login" {:action behave/login}]]))

(defn router
  [routes
   {{income-msg :income-msg
     fallback   :fallback} :request-data
    :as                    state}]
  (log/info "Processing: " (str/trim income-msg))
  (when-not (str/blank? income-msg)
    (let [match (r/match-by-path routes (first (str/split income-msg #"\s")))
          action (get-in match [:data :action] fallback)
          update-state (-> (xiana/flow->
                             state
                             action)
                           (xiana/extract))]
      (when-let [reply-fn (get-in update-state [:response-data :reply-fn])]
        (reply-fn update-state)))))

(def routing
  (partial router routes))

(defn chat-action
  [state]
  (xiana/ok
    (assoc-in state [:response-data :channel]
              {:on-receive (fn [ch msg]
                             (routing (update state :request-data
                                              merge {:ch         ch
                                                     :income-msg msg
                                                     :fallback   behave/broadcast
                                                     :channels   channels})))
               :on-open    (fn [ch]
                             (routing (update state :request-data
                                              merge {:ch         ch
                                                     :channels   channels
                                                     :income-msg "/welcome"})))
               :on-ping    (fn [ch data])
               :on-close   (fn [ch status] (swap! channels dissoc ch))
               :init       (fn [ch])})))
