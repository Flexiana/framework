(ns framework.db.storage
  (:require [com.stuartsierra.component :as component]))

(defrecord PostgreSQL [config]
  component/Lifecycle
  (start [this]
    (println "Start Postgresql " config))
  (stop [this]
    (println "Stop Postgresql")))

(defn postgresql
  [config]
  (->PostgreSQL config))
