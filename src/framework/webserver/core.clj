(ns framework.webserver.core
  "Lifecycle management of the webserver"
  (:require
    [clojure.tools.logging :as log]
    [clojure.tools.logging :as logger]
    [framework.config.core :as config]
    [framework.handler.core :refer [handler-fn]]
    [org.httpkit.server :as server]
    [piotr-yuxuan.closeable-map :as cm])
  (:import
    (java.lang
      AutoCloseable)))

;; web server reference
(defonce -webserver (atom {}))

(defrecord webserver [options server]
           AutoCloseable
           (close [this]
             (logger/info "Stop webserver" this)
             ((:server this))))

(defn- make
  "Web server instance."
  [options dependencies]
  (map->webserver
    {:options options
     :server  (server/run-server (handler-fn dependencies) options)}))

(defn stop
  "Stop web server."
  []
  ;; stop the server if necessary
  (when (not (empty? @-webserver))
    (when-let [-stop-server (get @-webserver :server)]
      (-stop-server))))

(defn start
  "Start web server."
  [dependencies]
  ;; stop the server
  (stop)
  ;; get server options
  (when-let [options (:webserver dependencies (:framework.app/web-server dependencies))]
    (when-let [server (make options dependencies)]
      (log/info "Server started with options: " options)
      (assoc dependencies :webserver server))))
