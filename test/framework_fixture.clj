(ns framework-fixture
  (:require
   [com.stuartsierra.component :as component]
   [framework.components.interceptors :as interceptors]
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
  {:web-server [:db]})

(def app-config
  (let [config (config/edn)]
    {:acl-cfg                 (select-keys config [:acl/permissions :acl/roles])
     :auth                    (:framework.app/auth config)
     :session-backend         (session-backend/init-in-memory-session)
     :router-interceptors     [(interceptors/message 0)]
     :controller-interceptors [(interceptors/message 1)
                               (interceptors/muuntaja)
                               (interceptors/message 2)
                               interceptors/params
                               (interceptors/message 3)
                               interceptors/session-interceptor
                               (interceptors/message 4)
                               interceptors/side-effect
                               (interceptors/message 5)
                               (interceptors/db-access)
                               (interceptors/message 6)]}))


(defn system
  [config app-config routes]
  {:db         (->postgresql config)
   :web-server (->web-server config app-config routes)})

(defn embedded-postgres!
  [config]
  (let [pg        (.start (EmbeddedPostgres/builder))
        pg-port   (.getPort pg)
        nuke-sql  (slurp "./Docker/init.sql")
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
                                     (system app-config routes)
                                     component/map->SystemMap
                                     (component/system-using sys-deps)))))

(defn std-system-fixture
  [f]
  (start st)
  (try
    (f)
    (finally
      (swap! st component/stop))))
