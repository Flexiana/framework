(ns framework.handler.core
  "Provides the default handler function"
  (:require
    [framework.interceptor.queue :as interceptor.queue]
    [framework.route.core :as router]
    [framework.state.core :as state]
    [org.httpkit.server :refer [as-channel]]
    [xiana.core :as xiana]))

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
  (let [router (router/router (:routes deps))]
    (fn handle*
      ([http-request]
       (try (if (:websocket? http-request)
              (let [state (state/make deps http-request)
                    queue (list #(interceptor.queue/execute % (:router-interceptors deps))
                                #(router %)
                                #(interceptor.queue/execute % (:web-socket-interceptors deps)))
                    flow (-> (xiana/apply-flow-> state queue)
                             (xiana/extract))
                    channel (get-in flow [:response-data :channel])]
                (if channel
                  (as-channel http-request channel)
                  (:response flow)))
              (let [state (state/make deps http-request)
                    queue (list #(interceptor.queue/execute % (:router-interceptors deps))
                                #(router %)
                                #(interceptor.queue/execute % (:controller-interceptors deps)))]
                (-> (xiana/apply-flow-> state queue)
                    (xiana/extract)
                    :response)))
            (catch Exception e (or (ex-data e)
                                   (.getMessage e)))))

      ([request respond _]
       (respond (handle* request))))))
