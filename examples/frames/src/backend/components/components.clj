(ns components
  (:require [app]
            [web-server]
            [com.stuartsierra.component :as component]
            [framework.config.core :as config]
            [framework.db.storage :as db.storage]))

(defn system
      [config]
      (let [pg-cfg (:framework.db.storage/postgresql config)
            app-cfg (:framework.app/ring config)
            web-server-cfg (:framework.app/web-server config)]

           (component/system-map
             :config config
             :db (db.storage/postgresql pg-cfg)
             :app-component (app/ring-app app-cfg)
             :web-server (web-server/web-server web-server-cfg))))

(defn -main
      [& args]
      (let [config (config/edn)]
           (component/start (system config))))
