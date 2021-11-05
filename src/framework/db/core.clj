(ns framework.db.core
  "Data source builder"
  (:require
    [framework.config.core :as config]
    [next.jdbc :as jdbc]))

(defn start
  "Creates datasource.
  When no parameter given, then resolves database specs from configuration"
  [db-spec]
  {:datasource (or (:datasource db-spec)
                   (jdbc/get-datasource db-spec))})
