(ns framework.components.core
  (:require [integrant.core :as ig]
            [framework.db.storage]
            [framework.config.core :as config]))

(defn -main
  [& args]
  (let [profile (keyword (or (System/getenv "PROFILE") "dev"))
        system (ig/init (config/edn profile))]
    (.addShutdownHook
      (Runtime/getRuntime)
      (Thread. ^Runnable (ig/halt! system)))))
