(ns framework.db.main
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [framework.config.core :as config]
    [migratus.core :as migratus]))

(defn help []
  (println "Available migratus commands:")
  (prn "create"
       "destroy"
       "down"
       "init"
       "migrate"
       "reset"
       "rollback"
       "up"))

(defn -main [& args]
  (let [[command param] args
        cfg (config/env)
        db (:framework.db.storage/postgresql cfg)
        config (assoc (:framework.db.storage/migration cfg) :db db)]
    (log/debug config)
    (if (str/blank? command)
      (help)
      (case (str/lower-case command)
        "create" (migratus/create config param)
        "destroy" (migratus/destroy config)
        "down" (migratus/down config param)
        "init" (migratus/init config)
        "migrate" (migratus/migrate config)
        "reset" (migratus/reset config)
        "rollback" (migratus/rollback config)
        "up" (migratus/up config param)
        (help)))))
