(ns framework.db.main
  (:require
    [clojure.string :as str]
    [taoensso.timbre :as log]
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
  (let [[command name type] args
        [_ & ids] args
        cfg (config/config)
        db (:xiana/postgresql cfg)
        config (assoc (:xiana/migration cfg) :db db)]
    (log/debug config)
    (if (str/blank? command)
      (help)
      (case (str/lower-case command)
        "create" (migratus/create config name (keyword type))
        "destroy" (migratus/destroy config)
        "down" (apply migratus/down config (map #(Long/parseLong %) ids))
        "init" (migratus/init config)
        "migrate" (migratus/migrate config)
        "reset" (migratus/reset config)
        "rollback" (migratus/rollback config)
        "up" (apply migratus/up config (map #(Long/parseLong %) ids))
        (help)))))
