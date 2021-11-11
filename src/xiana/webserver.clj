(ns xiana.webserver
  "Lifecycle management of the webserver"
  (:require
    [clojure.tools.logging :as logger]
    [org.httpkit.server :as server]
    [xiana.handler :refer [handler-fn]])
  (:import
    (java.lang
      AutoCloseable)))

(defrecord webserver
  [options server]
  AutoCloseable
  (close [this]
    (logger/info "Stop webserver" (:options this))
    ((:server this))))

(defn- make
  "Web server instance."
  [options dependencies]
  (map->webserver
    {:options options
     :server  (server/run-server (handler-fn dependencies) options)}))

(defn start
  "Start web server."
  [dependencies]
  ;; stop the server
  (when-let [webserver (get-in dependencies [:webserver :server])]
    (webserver))
  ;; get server options
  (when-let [options (:webserver dependencies (:xiana/web-server dependencies))]
    (when-let [server (make options dependencies)]
      (logger/info "Server started with options: " options)
      (assoc dependencies :webserver server))))
