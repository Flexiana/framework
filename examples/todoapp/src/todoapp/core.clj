(ns todoapp.core
  (:require
    [todoapp.controllers.index :as index]
    [todoapp.controllers.todos :as todos]
    [framework.config.core :as config]
    [framework.db.core :as db-core]
    [framework.route.core :as routes]
    [framework.webserver.core :as ws]
    [jsonista.core :as json]
    [reitit.core :as r]
    [ring.util.response :as ring]
    [xiana.core :as xiana]))

(def routes [["/" {:action index/handle-index}]
             ["/todos" {:action todos/fetch}]])

(defn system
  [config]
  (let [deps {:webserver        (:framework.app/web-server config)
              :routes           (routes/reset routes)
              :db               (db-core/start
                                 (:framework.db.storage/postgresql config))}]
    (assoc deps :web-server (ws/start deps))))

(defn -main
  [& _args]
  (let [config (config/env)]
    (system config)))
