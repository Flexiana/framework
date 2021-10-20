(ns framework.handler.core
  "Provides the default handler function"
  (:require
    [framework.interceptor.queue :as interceptor.queue]
    [framework.route.core :as route]
    [framework.state.core :as state]
    [xiana.core :as xiana]))

(defn handler-fn
  "Returns handler function for jetty, which  do the routing, and executes interceptors and given action.

   Execution order:
    router interceptors: enters in order
    router interceptors leaves in reversed order
      routing
    around interceptors enters in order
    controller interceptors enters in order
    inside interceptors enters in order
      action
    inside interceptors leaves in reversed order
    controller interceptors leaves in reversed order
    around interceptors leaves in reversed order"
  [deps]
  (fn handle*
    ([http-request]
     (if-not (:websocket? http-request)
       (let [state (state/make deps http-request)
             queue (list #(interceptor.queue/execute % (:router-interceptors deps))
                         #(route/match %)
                         #(interceptor.queue/execute % (:controller-interceptors deps)))]
         (-> (xiana/apply-flow-> state queue)
             ;; extract
             (xiana/extract)
             ;; get the response
             (get :response)))
       (let [state (state/make deps http-request)
             match (xiana/extract (route/match state))
             method (:request-method http-request)
             action (get-in match [:request-data :action])]
         (action http-request))))
    ([request respond _]
     (respond (handle* request)))))
