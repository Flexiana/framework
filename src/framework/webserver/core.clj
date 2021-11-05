(ns framework.webserver.core
  "Lifecycle management of the webserver"
  (:require
    [clojure.tools.logging :as log]
    [framework.app :as-alias app]
    [framework.config.core :as config]
    [framework.handler.core :refer [handler-fn]]
    [org.httpkit.server :as server])
  (:import
    (java.lang
      AutoCloseable)))

;; web server reference
(defonce -webserver (atom {}))

(defn- make
  "Web server instance."
  [options dependencies]
  {:options options
   :server  (server/run-server (handler-fn dependencies) options)})

(defn stop
  "Stop web server."
  []
  ;; stop the server if necessary
  (when (not (empty? @-webserver))
    (when-let [-stop-server (get @-webserver :server)]
      (-stop-server))))

(defn start
  [{::app/keys [web-server]
    :as config}]
  (let [web-server-config (assoc web-server
                                 :legacy-return-value? false)
        server (server/run-server
                 (handler-fn config)
                 web-server-config)]
    (assoc config :web-server
           (reify AutoCloseable
             (close [this]
               (server/server-stop! server))))))
