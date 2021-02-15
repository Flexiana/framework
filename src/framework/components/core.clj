(ns framework.components.core
  (:require
    [com.stuartsierra.component :as component]
    [config.core :refer [load-env]]
    [framework.db.storage :as db.storage]))

(defn system
  [env]
  (-> env
      (update :framework.db.storage/postgresql db.storage/->PostgreSQL)
      component/map->SystemMap))

(defn -main
  [& _args]
  (-> (load-env)
      system
      component/start))
