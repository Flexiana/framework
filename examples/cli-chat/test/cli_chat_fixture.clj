(ns cli-chat-fixture
  (:require
   [framework.db.core :as db]
   [framework.config.core :as config]
   [cli-chat.core :refer [->system]]))

(defn std-system-fixture
  [app-cfg f]
  (with-open [_ (-> (config/config)
                    (merge app-cfg)
                    db/docker-postgres!
                    ->system)]
    (f)))
