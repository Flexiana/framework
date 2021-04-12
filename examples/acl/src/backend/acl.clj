(ns acl
  (:require
    [com.stuartsierra.component :as component]
    [controllers.comments :as comments-controllers]
    [controllers.index :as index]
    [controllers.posts :as posts-controllers]
    [controllers.re-frame :as re-frame]
    [controllers.users :as users-controllers]
    [framework.components.app.core :as xiana.app]
    [framework.components.interceptors :as interceptors]
    [framework.components.router.core :as xiana.router]
    [framework.components.session.backend :as session-backend]
    [framework.components.web-server.core :as xiana.web-server]
    [framework.config.core :as config]
    [framework.db.storage :as db.storage]
    [interceptors.load-user :as user]
    [nrepl.server :refer [start-server stop-server]]
    [reitit.ring :as ring]))

(def routes
  [["/" {:action index/handle-index}]
   ["/re-frame" {:action re-frame/handle-index}]
   ["/assets/*" (ring/create-resource-handler)]
   ["" {:handler xiana.app/default-handler}
    ["/posts" {:get    {:action posts-controllers/fetch}
               :put    {:action posts-controllers/add}
               :post   {:action posts-controllers/update-post}
               :delete {:action posts-controllers/delete-post}}]
    ["/posts/ids" {:post {:action posts-controllers/fetch-by-ids}}]
    ["/posts/comments" {:get {:action posts-controllers/fetch-with-comments}}]
    ["/comments" {:get    {:action comments-controllers/fetch}
                  :put    {:action comments-controllers/add}
                  :post   {:action comments-controllers/update-comment}
                  :delete {:action comments-controllers/delete-comment}}]
    ["/users" {:get    {:action users-controllers/fetch}
               :put    {:action users-controllers/add}
               :post   {:action users-controllers/update-user}
               :delete {:action users-controllers/delete-user}}]
    ["/users/posts" {:get {:action users-controllers/fetch-with-posts}}]
    ["/users/posts/comments" {:get {:action users-controllers/fetch-with-posts-comments}}]]])

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
        :app (xiana.app/make-app {:config app-cfg
                                  :acl-cfg acl-cfg
                                  :session-backend session-bcknd
                                  :router-interceptors []
                                  :controller-interceptors [;interceptors/log
                                                            (interceptors/muuntaja)
                                                            interceptors/params
                                                            (interceptors/require-logged-in)
                                                            interceptors/session-interceptor
                                                            interceptors/view
                                                            (interceptors/db-access user/load-user)
                                                            (interceptors/acl-restrict views.common/not-allowed)]})
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
