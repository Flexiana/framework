(ns framework.webserver.core
  (:require
    [framework.config.core :as config]
    [framework.interceptor.queue :as interceptor.queue]
    [framework.route.core :as route]
    [framework.state.core :as state]
    [ring.adapter.jetty :as jetty]
    [xiana.core :as xiana]))

;; web server reference
(defonce -webserver (atom {}))

(defn handler-fn
  "Return jetty server handler function."
  [deps]
  (fn [http-request]
    (let [state (state/make deps http-request)
          queue (list #(route/match %)
                      #(interceptor.queue/execute % (:controller-interceptors deps)))]
      (-> (xiana/apply-flow-> state queue)
          ;; extract
          (xiana/extract)
          ;; get the response
          (get :response)))))

(defn- make
  "Web server instance."
  [options dependencies]
  {:options options
   :server  (jetty/run-jetty (handler-fn dependencies) options)})

(defn stop
  "Stop web server."
  []
  ;; stop the server if necessary
  (when (not (empty? @-webserver))
    (.stop (get @-webserver :server))))

(defn start
  "Start web server."
  [dependencies]
  ;; stop the server
  (stop)
  ;; get server options
  (when-let [options (merge (config/get-spec :webserver) (:webserver dependencies))]
    ;; tries to initialize the web-server if we have the
    ;; server specification (its options)
    (swap! -webserver merge (make options dependencies))))
