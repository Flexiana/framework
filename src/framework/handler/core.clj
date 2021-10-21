(ns framework.handler.core
  "Provides the default handler function"
  (:require
    [framework.interceptor.queue :as interceptor.queue]
    [framework.route.core :as route]
    [framework.state.core :as state]
    [org.httpkit.server :as s]
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
     (let [state (state/make deps http-request)
           queue (list #(interceptor.queue/execute % (:router-interceptors deps))
                       #(route/match %)
                       #(interceptor.queue/execute % (:controller-interceptors deps)))]
       (if (:websocket? http-request)
         (let [flow (-> (xiana/apply-flow-> state queue)
                        (xiana/extract))
               channel (get-in flow [:response-data :channel])]
           (if channel
             (s/as-channel http-request channel)
             (:response flow)))
         (-> (xiana/apply-flow-> state queue)
             (xiana/extract)
             :response))))

    ([request respond _]
     (respond (handle* request)))))
