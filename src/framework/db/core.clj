(ns framework.db.core
  (:require
    [next.jdbc :as jdbc]
    [clojure.pprint]
    [framework.config.core :as config]))

;; database instance reference
(defonce db (atom {}))

(defn- make
  "Return database instance map."
  [spec]
  (let [datasource (jdbc/get-datasource spec)]
    {:datasource datasource
     :connection (jdbc/get-connection datasource)}))

(defn start
  "Start database instance.
  Get the database specification from
  the 'edn' configuration file in case of
  db-spec isn't set."
  ([] (start nil))
  ([db-spec]
   (when-let [spec (or db-spec (config/get-spec :database))]
     (swap! db merge (make spec)))))
