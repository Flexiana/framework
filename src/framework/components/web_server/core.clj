(ns framework.components.web-server.core
  (:require
    [com.stuartsierra.component :as component]
    [ring.adapter.jetty :as jetty])
  (:import
    (org.eclipse.jetty.server
      Server)))

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
  "DEPRECATED"
  [config]
  (map->WebServer {:config config}))

(defn ->web-server
  [{web-cfg :framework.app/web-server
    :as     config}]
  (with-meta config
    `{component/start ~(fn [{{handler :handler} :app
                             :as                this}]
                         (assoc this :web-server
                           (jetty/run-jetty handler web-cfg)))
      component/stop  ~(fn [{:keys [^Server web-server]
                             :as   this}]
                         (.stop web-server)
                         (dissoc this :web-server))}))
