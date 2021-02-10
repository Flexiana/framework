(ns framework.components.core
  (:require
   [com.stuartsierra.component :as component]
   [framework.db.storage :as db.storage]
   [config.core :refer [load-env]]))


(defn system
  [env]
  (-> env
      (update :framework.db.storage/postgresql db.storage/->PostgreSQL)
      component/map->SystemMap))

(defn -main
  [& _args]
  (let [env (load-env)]
    (-> env
        system
        component/start)))
