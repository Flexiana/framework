(ns migrator-fixture
  (:require
    [migrator.core :refer [system]]
    [framework.db.test-support :as test-support]
    [framework.config.core :as config]
    [framework.webserver.core :as ws]))

(defn std-system-fixture
  [config f]
  (try
    (-> (merge (config/env) config)
        ;(test-support/docker-postgres! [(slurp "Docker/init.sql")])
        (test-support/embedded-postgres! [(slurp "Docker/init.sql")])
        test-support/migrate!
        system)
    (f)
    (finally
      (ws/stop))))
