(ns framework-fixture
  (:require
    [framework.config.core :as config]
    [framework.db.core :as db-core]
    [framework.rbac.core :as rbac]
    [framework.route.core :as routes]
    [framework.session.core :as session-backend]
    [framework.sse.core :as sse]
    [framework.webserver.core :as ws]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [xiana.commons :refer [update-key]]))

(defn system
  [config]
  (closeable-map
    (-> config
        (update-key :framework.app/auth :auth)
        (session-backend/init-in-memory)
        (db-core/start)
        (db-core/migrate!)
        routes/reset
        rbac/init
        sse/init
        ws/start)))

(defn std-system-fixture
  [config f]
  (with-open [test-system (system (config/env config))]
    (f)))

(defonce ttest (atom nil))

(comment

  (reset! ttest (system (config/env)))

  (comment (.close @ttest)))
