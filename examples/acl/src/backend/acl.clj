(ns acl
  (:require
    [com.stuartsierra.component :as component]
    [controllers.index :as index]
    [controllers.posts :as posts-controllers]
    [controllers.re-frame :as re-frame]
    [custom-handlers]
    [empty-controller :as empty]
    [framework.components.app.core :as xiana.app]
    [framework.components.router.core :as xiana.router]
    [framework.components.session.backend :as session-backend]
    [framework.components.web-server.core :as xiana.web-server]
    [framework.config.core :as config]
    [framework.db.storage :as db.storage]
    [interceptors]
    [muuntaja.interceptor]
    [nrepl.server :refer [start-server stop-server]]
    [reitit.ring :as ring]))

(def routes
  [["/" {:controller index/handle-index}]
   ["/re-frame" {:controller re-frame/handle-index}]
   ["/assets/*" (ring/create-resource-handler)]
   ["" {:handler xiana.app/default-handler}
    ["/posts" {:get    {:controller posts-controllers/fetch}
               :put    {:controller posts-controllers/add}
               :post   {:controller posts-controllers/update-post}
               :delete {:controller posts-controllers/delete-post}}]
    ["/posts/ids" {:post {:controller posts-controllers/fetch-by-ids}}]
    ["/posts/comments" {:get {:controller posts-controllers/fetch-with-comments}}]
    ["/comments" {:get    {:controller empty/controller}
                  :put    {:controller empty/controller}
                  :post   {:controller empty/controller}
                  :delete {:controller empty/controller}}]
    ["/comments/ids" {:post {:controller empty/controller}}]
    ["/users" {:get    {:controller empty/controller}
               :put    {:controller empty/controller}
               :post   {:controller empty/controller}
               :delete {:controller empty/controller}}]
    ["/users/ids" {:post {:controller empty/controller}}]
    ["/users/posts" {:get {:controller empty/controller}}]
    ["/users/posts/comments" {:get {:controller empty/controller}}]]])

(defn system
  [config]
  (let [pg-cfg (:framework.db.storage/postgresql config)
        app-cfg (:framework.app/ring config)
        session-bcknd (session-backend/make-session-store)
        acl-cfg (select-keys config [:acl/permissions :acl/roles])
        web-server-cfg (:framework.app/web-server config)]
    (->
      (component/system-map
        :config config
        :db (db.storage/postgresql pg-cfg)
        :router (xiana.router/make-router routes)
        :session-backend session-bcknd
        :acl-cfg acl-cfg
        :app (xiana.app/make-app app-cfg
                                 acl-cfg
                                 session-bcknd
                                 []
                                 [;interceptors/log
                                  interceptors/muuntaja
                                  interceptors/params
                                  interceptors/require-logged-in
                                  interceptors/session-interceptor
                                  interceptors/view
                                  interceptors/db-access
                                  interceptors/acl-restrict])
                                  ;interceptors/query-builder])
        :web-server (xiana.web-server/make-web-server web-server-cfg))
      (component/system-using
        {:router     [:db]
         :app        [:router :db :acl-cfg :session-backend]
         :web-server [:app]}))))

(defonce server (start-server :port 7888))

(defn -main
  [& _args]
  (let [config (config/edn)]
    (component/start (system config))))