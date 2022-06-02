(ns xiana.handler
  "Provides the default handler function"
  (:require
    [ring.adapter.jetty9 :as jetty]
    [xiana.core :as xiana]
    [xiana.interceptor.queue :as interceptor.queue]
    [xiana.route :as route]
    [xiana.state :as state]))

(defn handler-fn
  "Returns handler function for server, which  do the routing, and executes interceptors and given action.

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
     (let [websocket? (jetty/ws-upgrade-request? http-request)
           state (state/make deps http-request)
           queue (list #(interceptor.queue/execute % (:router-interceptors deps))
                       #(route/match %)
                       #(interceptor.queue/execute % (if websocket?
                                                       (:web-socket-interceptors deps)
                                                       (:controller-interceptors deps))))
           error-interceptors (->>
                                (:error-interceptors deps [])
                                (map #(interceptor.queue/interceptor->fn :leave %))
                                (remove nil?))
           flow (xiana/apply-flow-> state queue)
           result (if (and (xiana/error? flow) (seq error-interceptors))
                    (-> flow
                        xiana/extract
                        (xiana/apply-flow-> error-interceptors)
                        xiana/extract)
                    (xiana/extract flow))
           channel (get-in result [:response-data :channel])]
       (if (and websocket? channel)
         (jetty/ws-upgrade-response channel)
         (:response result))))
    ([request respond _]
     (respond (handle* request)))))
