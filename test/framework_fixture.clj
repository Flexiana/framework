(ns framework-fixture
  (:require
    [com.stuartsierra.component :as component]
    [framework.components.app.core :refer [->app route]]
    [framework.components.interceptors :as interceptors]
    [framework.components.router.core :refer [->router]]
    [framework.components.session.backend :as session-backend]
    [framework.components.web-server.core :refer [->web-server]]
    [framework.config.core :as config]
    [framework.db.storage :refer [->postgresql]]
    [next.jdbc :as jdbc])
  (:import
    (com.opentable.db.postgres.embedded
      EmbeddedPostgres)))

(def routes
  ["/users" {:get #(assoc % :response {:status 200 :body "Ok"})}])

(def sys-deps
  {:router     [:db]
   :app        [:router :db]
   :web-server [:app]})

(defn system
  [config]
  (let [acl-cfg (select-keys config [:acl/permissions :acl/roles])
        session-bcknd (session-backend/init-in-memory-session)]
    {:db         (->postgresql config)
     :router     (->router config routes)
     :app        (->app {:acl-cfg                 acl-cfg
                         :auth                    (:framework.app/auth config)
                         :session-backend         session-bcknd
                         :router-interceptors     []
                         :controller-interceptors [;interceptors/log
                                                   (interceptors/muuntaja)
                                                   interceptors/params
                                                   ;(interceptors/require-logged-in)
                                                   interceptors/session-interceptor
                                                   ;interceptors/view
                                                   interceptors/side-effect
                                                   (interceptors/db-access)]})
                                                   ;(interceptors/acl-restrict {:prefix "/api"})]})
     :web-server (->web-server config)}))

(defn embedded-postgres!
  [config]
  (let [pg (.start (EmbeddedPostgres/builder))
        pg-port (.getPort pg)
        nuke-sql (slurp "./Docker/init.sql")
        db-config (-> config
                      :framework.db.storage/postgresql
                      (assoc
                        :port pg-port
                        :embedded pg
                        :subname (str "//localhost:" pg-port "/framework")))]
    (jdbc/execute! (dissoc db-config :dbname) [nuke-sql])
    (assoc config :framework.db.storage/postgresql db-config)))

(defonce st (atom {}))

(defn start
  [state]
  (reset! state (component/start (-> (config/edn)
                                     embedded-postgres!
                                     system
                                     component/map->SystemMap
                                     (component/system-using sys-deps)))))

(defn std-system-fixture
  [f]
  (start st)
  (try
    (f)
    (finally
      (swap! st component/stop))))
