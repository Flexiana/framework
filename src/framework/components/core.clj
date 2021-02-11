(ns framework.components.core
  (:require
    [com.stuartsierra.component :as component]
    [framework.config.core :as config]
    [framework.db.storage :as db.storage]))


(defn system
  [config]
  (let [pg-cfg (:framework.db.storage/postgresql config)]
    (component/system-map
      :config config
      :db (db.storage/postgresql pg-cfg))))


(defn -main
  [& args]
  (let [config (config/edn)]
    (component/start (system config))))
