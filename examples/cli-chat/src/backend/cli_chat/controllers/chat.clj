(ns cli-chat.controllers.chat
  (:require
    [cli-chat.controller-behaviors.chat :as behave]
    [clojure.pprint]
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [framework.handler.core]
    [framework.interceptor.core :as interceptors]
    [framework.interceptor.queue :as queue]
    [reitit.core :as r]
    [xiana.core :as xiana]))

(defonce channels (atom {}))

(defn keyceptor
  [& keyz]
  {:enter (fn [state]
            (log/info keyz (get-in state keyz))
            (xiana/ok state))
   :leave (fn [state]
            (log/info keyz (get-in state keyz))
            (xiana/ok state))})

(defn fallback
  [{{income-msg :income-msg} :request-data
    :as                      state}]

  (if (str/starts-with? income-msg "/")
    (xiana/ok
      (update state :response-data merge {:reply-fn behave/send-multi-line
                                          :reply    (str "Invalid command: " income-msg)}))
    (behave/broadcast state)))

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
            {:data {:default-interceptors [(keyceptor :request-data :income-msg)]}}))

(defn router
  [routes
   {{income-msg :income-msg
     fallback   :fallback} :request-data
    :as                    state}]

  (when-not (str/blank? income-msg)
    (let [match (r/match-by-path routes (first (str/split income-msg #"\s")))
          action (get-in match [:data :action] fallback)
          interceptors (get-in match [:data :interceptors])
          default-interceptors (get-in match [:data :default-interceptors])
          _ (or (get-in match [:data :hide])
                (log/info "Processing: " (str/trim income-msg)))
          routing-state (update state :request-data assoc
                                :action action
                                :interceptors interceptors)
          update-state (-> (xiana/flow->
                             routing-state
                             (queue/execute default-interceptors))
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
                                                     :fallback   fallback
                                                     :channels   channels})))
               :on-open    (fn [ch]
                             (routing (update state :request-data
                                              merge {:ch         ch
                                                     :channels   channels
                                                     :income-msg "/welcome"})))
               :on-ping    (fn [ch data])
               :on-close   (fn [ch status] (swap! channels dissoc ch))
               :init       (fn [ch])})))
