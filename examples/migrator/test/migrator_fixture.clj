(ns migrator-fixture
  (:require
    [framework.config.core :as config]
    [framework.db.test-support :as test-support]
    [framework.webserver.core :as ws]
    [migrator.core :refer [system]]))

(defn std-system-fixture
  [config f]
  (try
    (-> (merge (config/env) config)
        ;(test-support/docker-postgres! [(slurp "Docker/init.sql")])
        (test-support/embedded-postgres! [(slurp "Docker/init.sql")])
        system)
    (f)
    (finally
      (ws/stop))))
