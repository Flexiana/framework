(ns framework.db.core
  "Data source builder"
  (:require
    [framework.config.core :as config]
    [next.jdbc :as jdbc]))

(defn start
  "Creates datasource.
  When no parameter given, then resolves database specs from configuration"
  ([] (start nil))
  ([db-spec]
   (when-let [spec (or db-spec (config/get-spec :database))]
     {:datasource (jdbc/get-datasource spec)})))
