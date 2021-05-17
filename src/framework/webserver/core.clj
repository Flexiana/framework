(ns framework.webserver.core
  (:require
   [xiana.core :as xiana]
   [ring.adapter.jetty :as jetty]
   [framework.route.core :as route]
   [framework.state.core :as state]
   [framework.config.core :as config]
   [framework.interceptor.queue :as interceptor.queue])
  (:import
   (org.eclipse.jetty.server Server)))

;; web server reference
(defonce -webserver (atom {}))

(defn handler-fn
  "Return jetty server handler function."
  [interceptors]
  (fn [http-request]
    (->
     (xiana/flow->
      ;; make the initial context
      (state/make http-request)
      ;; update context with the route match template
      (route/match)
      ;; execute the interceptors queue
      (interceptor.queue/execute interceptors))
     ;; unwrap/extract the context container
     (xiana/extract)
     ;; get the response
     (get :response))))

(defn- make
  "Web server instance."
  [options interceptors]
  {:options options
   :server  (jetty/run-jetty (handler-fn interceptors) options)})

(defn stop
  "Stop web server."
  []
  ;; stop the server if necessary
  (when (not (empty? @-webserver))
    (.stop (get @-webserver :server))))

(defn start
  "Start web server."
  [interceptors]
  ;; stop the server
  (stop)
  ;; get server options
  (when-let [options (config/get-spec :webserver)]
    ;; tries to initialize the web-server if we have the
    ;; server specification (its options)
    (swap! -webserver
           (fn [m]
             (merge m (make options interceptors))))))
