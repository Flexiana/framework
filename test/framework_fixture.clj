(ns framework-fixture
  "Sample application to test the framework startup and functions"
  (:require
    [clj-test-containers.core :as tc]
    [com.stuartsierra.component :as component]
    [framework.components.interceptors :as interceptors]
    [framework.components.session.backend :as session-backend]
    [framework.components.web-server.core :refer [->web-server route]]
    [framework.config.core :as config]
    [framework.db.storage :refer [->postgresql]]
    [framework.one-endpoint-functions :as f-map]
    [framework.test-interceptors :as ti]
    [next.jdbc :as jdbc]
    [xiana.core :as xiana])
  (:import
    (com.opentable.db.postgres.embedded
      EmbeddedPostgres)))

(def routes
  [["/users" {:get {:action  f-map/get-user-controller
                    :handler route}}]
   ["/interceptor" {:get {:handler      route
                          :action       #(xiana/ok (update % :response conj {:status 200 :body "Ok"}))
                          :interceptors [ti/test-interceptor]}}]
   ["/action" {:post {:handler route}}]
   ["/test-override" {:post {:handler      route
                             :action       #(xiana/ok (update % :response conj {:status 200 :body "Ok"}))
                             :interceptors {:override [ti/test-override]}}}]
   ["/session" {:post {:handler      route
                       :action       #(xiana/ok (update % :response conj {:status 200 :body "Ok"}))
                       :interceptors {:override [(interceptors/muuntaja)
                                                 ;interceptors/log
                                                 interceptors/params
                                                 interceptors/session-interceptor
                                                 ti/response-session
                                                 (ti/single-entry f-map/action-map "/session")
                                                 interceptors/view
                                                 interceptors/side-effect
                                                 (interceptors/db-access)
                                                 (interceptors/acl-restrict)]}}}]])

(def sys-deps
  {:web-server [:db]})

(def app-config
  (let [config (config/edn)]
    {:acl-cfg                 (select-keys config [:acl/permissions :acl/roles])
     :auth                    (:framework.app/auth config)
     :session-backend         (session-backend/init-in-memory-session)
     :router-interceptors     []
     :controller-interceptors [(interceptors/muuntaja)
                               ;interceptors/log
                               interceptors/params
                               interceptors/session-interceptor
                               (ti/single-entry f-map/action-map "/action")
                               interceptors/view
                               interceptors/side-effect
                               (interceptors/db-access)
                               (interceptors/acl-restrict)]}))

(defn system
  [config app-config routes]
  {:db         (->postgresql config)
   :web-server (->web-server config app-config routes)})

(defn embedded-postgres!
  [config]
  (let [pg (.start (EmbeddedPostgres/builder))
        pg-port (.getPort pg)
        nuke-sql (slurp "./Docker/init.sql")
        init-sql (slurp "./test/resources/init.sql")
        db-config (-> config
                      :framework.db.storage/postgresql
                      (assoc
                        :port pg-port
                        :embedded pg
                        :subname (str "//localhost:" pg-port "/framework")))]
    (jdbc/execute! (dissoc db-config :dbname) [nuke-sql])
    (jdbc/execute! db-config [init-sql])
    (assoc config :framework.db.storage/postgresql db-config)))

(defn docker-postgres
  [config]
  (let [container (-> (tc/create {:image-name    "postgres:11.5-alpine"
                                  :exposed-ports [5432]})
                      ;:env-vars      {"POSTGRES_PASSWORD" "verysecret"}})
                      ;(tc/bind-filesystem! {:host-path      "/tmp"
                      ;                      :container-path "/opt"
                      ;                      :mode           :read-only})
                      (tc/start!))
        port (get (:mapped-ports container) 5432)
        nuke-sql (slurp "./Docker/init.sql")
        init-sql (slurp "./test/resources/init.sql")
        db-config (-> config
                      :framework.db.storage/postgresql
                      (assoc
                        :port port
                        :embedded container
                        :subname (str "//localhost:" port "/framework")))]
    (jdbc/execute! (dissoc db-config :dbname) [nuke-sql])
    (jdbc/execute! db-config [init-sql])
    (assoc config :framework.db.storage/postgresql db-config)))

(defonce st (atom {}))

(defn start
  [state]
  (reset! state (component/start (-> (config/edn)
                                     ;embedded-postgres!
                                     docker-postgres
                                     (system app-config routes)
                                     component/map->SystemMap
                                     (component/system-using sys-deps)))))

(defn stop
  [state]
  (swap! state component/stop))

(defn std-system-fixture
  [f]
  (start st)
  (try
    (f)
    (finally
      (stop st))))

(comment
  (stop st)
  (start st))
