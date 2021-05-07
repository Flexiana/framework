(ns framework.components.core
  (:require
    [framework.config.core :as config]
    [framework.db.storage :as db.storage]
    [com.stuartsierra.component :as component]))

(defn system
  [env]
  (-> env
      (update :framework.db.storage/postgresql db.storage/->PostgreSQL)
      component/map->SystemMap))

(defn -main
  [& _args]
  (-> (config/read-edn-file nil)
      system
      component/start))
