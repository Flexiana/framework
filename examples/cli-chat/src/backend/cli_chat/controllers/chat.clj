(ns cli-chat.controllers.chat
  (:require
    [cli-chat.controller-behaviors.chat :as behave]
    [framework.interceptor.core :as interceptors]
    [framework.websockets.core :refer [router string->]]
    [reitit.core :as r]
    [xiana.core :as xiana]))

(defonce channels (atom {}))

(def routes
  (r/router [["/help" {:action behave/help}]
             ["/welcome" {:action behave/welcome}]
             ["/me" {:action behave/me}]
             ["/to" {:action behave/to}]
             ["/login" {:action       behave/login
                        :interceptors {:inside [interceptors/side-effect
                                                interceptors/db-access]}
                        :hide         true}]
             ["/sign-up" {:action       behave/sign-up
                          :interceptors {:inside [interceptors/side-effect
                                                  interceptors/db-access]}
                          :hide         true}]]
            {:data {:default-interceptors [(interceptors/message "Incoming message...")]}}))

(def routing
  (partial router routes string->))

(defn chat-action
  [state]
  (xiana/ok
    (assoc-in state [:response-data :channel]
              {:on-receive (fn [ch msg]
                             (routing (update state :request-data
                                              merge {:ch         ch
                                                     :income-msg msg
                                                     :fallback   behave/fallback
                                                     :channels   channels})))
               :on-open    (fn [ch]
                             (routing (update state :request-data
                                              merge {:ch         ch
                                                     :channels   channels
                                                     :income-msg "/welcome"})))
               :on-ping    (fn [ch data])
               :on-close   (fn [ch status] (swap! channels dissoc ch))
               :init       (fn [ch])})))
