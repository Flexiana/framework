(ns framework.webserver.core
  "Lifecycle management of the webserver"
  (:require
    [framework.handler.core :refer [handler-fn]]
    [ring.adapter.jetty9 :as jetty]
    [taoensso.timbre :as log])
  (:import
    (java.lang
      AutoCloseable)))

(defrecord webserver
  [options server]
  AutoCloseable
  (close [this]
    (log/info "Stop webserver" (:options this))
    (jetty/stop-server (:server this))))

(defn- make
  "Web server instance."
  [dependencies]
  (let [options (:webserver dependencies (:xiana/web-server dependencies))]
    (map->webserver
      {:options options
       :server  (jetty/run-jetty (handler-fn dependencies) options)})))

(defn start
  "Start web server."
  [dependencies]
  ;; stop the server
  (when-let [webserver (get-in dependencies [:webserver :server])]
    (webserver))
  ;; get server options
  (when-let [server (make dependencies)]
    (log/info "Server started with options: " (:options server))
    (assoc dependencies :webserver server)))
