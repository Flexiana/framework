(ns xiana.db.migrate
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [migratus.core :as migratus]
    [xiana.config :as config]))

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
        cfg (config/config)
        db (:xiana/postgresql cfg)
        config (assoc (:xiana/migration cfg) :db db)]
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
