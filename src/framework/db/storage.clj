(ns framework.db.storage
  (:require
    [next.jdbc :as jdbc])
  (:import
    (com.stuartsierra.component
      Lifecycle)))

(defrecord PostgreSQL
  [config]
  Lifecycle
  (start [this]
         (let [datasource (jdbc/get-datasource config)]
           (-> this
               (assoc :datasource datasource)
               (assoc :connection (jdbc/get-connection datasource)))))
  (stop [this]
        (dissoc this :datasource :connection)))

(defn postgresql
  [config]
  (->PostgreSQL config))
