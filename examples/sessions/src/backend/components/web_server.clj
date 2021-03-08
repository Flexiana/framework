(ns web-server
  (:require
    [com.stuartsierra.component :as component]
    [ring.adapter.jetty :as jetty]))

(defrecord WebServer
  [http-server app config]
  component/Lifecycle
  (start
    [this]
    (assoc this :http-server
      (jetty/run-jetty (:handler app) config)))
  (stop
    [this]
    (.stop http-server)
    this))

(defn make-web-server
  [config]
  (map->WebServer {:config config}))
