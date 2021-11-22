(ns cli-chat-fixture
  (:require
    [cli-chat.core :refer [->system]]
    [framework.config.core :as config]
    [framework.db.core :as db]))

(defn std-system-fixture
  [app-cfg f]
  (with-open [_ (-> (config/config)
                    (merge app-cfg)
                    db/docker-postgres!
                    ->system)]
    (f)))
