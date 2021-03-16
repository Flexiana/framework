(ns framework.db.storage
  (:require
    [com.stuartsierra.component :as component]
    [next.jdbc :as jdbc]))

(defrecord PostgreSQL
  [config]
  component/Lifecycle
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
