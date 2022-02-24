(ns acl-fixture
  (:require
    [acl]
    [xiana.config :as config]
    [xiana.db :as db]))

(defn std-system-fixture
  [f]
  (with-open [_ (-> (config/config)
                    (merge acl/app-cfg)
                    db/docker-postgres!
                    acl/->system)]
    (f)))
