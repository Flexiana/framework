(ns cli-chat-fixture
  (:require
    [cli-chat.core :refer [system]]
    [framework.config.core :as config]
    [framework.db.test-support :refer [docker-postgres!
                                       embedded-postgres!
                                       migrate!]]
    [framework.webserver.core :as ws]))

(defn std-system-fixture
  [config f]
  (try
    (-> (config/env)
        (merge config)
        (embedded-postgres! [(slurp "./Docker/init.sql")])
        ;(docker-postgres! [(slurp "./Docker/init.sql")])
        migrate!
        system)
    (f)
    (finally
      (ws/stop))))
