(ns framework.webserver.core
  "Lifecycle management of the webserver"
  (:require
    [framework.config.core :as config]
    [framework.handler.core :refer [handler-fn]]
    [ring.adapter.jetty :as jetty]))

;; web server reference
(defonce -webserver (atom {}))

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
