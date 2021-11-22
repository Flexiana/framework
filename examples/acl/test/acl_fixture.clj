(ns acl-fixture
  (:require
    [acl]
    [framework.config.core :as config]
    [framework.db.core :as db]))

(defn std-system-fixture
  [f]
  (with-open [_ (-> (config/config)
                    (merge acl/app-cfg)
                    db/docker-postgres!
                    acl/->system)]
    (f)))
