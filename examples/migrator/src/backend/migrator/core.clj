(ns migrator.core
  (:require
    [framework.config.core :as config]
    [framework.db.core :as db]
    [framework.db.sql :as sql]
    [framework.interceptor.core :as interceptors]
    [framework.rbac.core :as rbac]
    [framework.route.core :as routes]
    [framework.session.core :as session]
    [framework.webserver.core :as ws]
    [migrator.controllers.index :as index]
    [migrator.controllers.re-frame :as re-frame]
    [migratus.core :as migratus]
    [reitit.ring :as ring]))

(def routes
  [["/" {:action index/handle-index}]
   ["/re-frame" {:action re-frame/handle-index}]
   ["/assets/*" (ring/create-resource-handler {:path "/"})]])

(defn system
  [config]
  (let [migration-cfg (assoc (:framework.db.storage/migration config)
                             :db (:framework.db.storage/postgresql config))
        deps {:routes                  (routes/reset routes)
              :db                      (db/start (:framework.db.storage/postgresql config))
              :webserver               (:framework.app/web-server config)
              :role-set                (rbac/init (:framework.app/role-set config))
              :auth                    (:framework.app/auth config)
              :session-backend         (session/init-in-memory)
              :router-interceptors     []
              :controller-interceptors [(interceptors/muuntaja)
                                        interceptors/params
                                        session/guest-session-interceptor
                                        interceptors/view
                                        interceptors/side-effect
                                        sql/db-access
                                        rbac/interceptor]}]
    (migratus/migrate migration-cfg)
    (assoc deps :webserver (ws/start deps))))

(defonce system-map (atom {}))

(defn -main
  [& _args]
  {:pre (even? (count _args))}
  (reset! system-map (-> (config/env)
                         (merge _args)
                         system)))
