(ns acl
  (:require
    [com.stuartsierra.component :as component]
    [controller-behaviors.comments :as comments-behaviors]
    [controller-behaviors.posts :as posts-behaviors]
    [controllers.index :as index]
    [controllers.re-frame :as re-frame]
    [empty-controller :as empty]
    [framework.components.app.core :as xiana.app]
    [framework.components.router.core :as xiana.router]
    [framework.components.session.backend :as session-backend]
    [framework.components.web-server.core :as xiana.web-server]
    [framework.config.core :as config]
    [framework.db.storage :as db.storage]
    [interceptors]
    [nrepl.server :refer [start-server stop-server]]
    [reitit.ring :as ring]))

(def routes
  [["/" {:controller index/handle-index}]
   ["/re-frame" {:controller re-frame/handle-index}]
   ["/posts" {:get    {:handler    xiana.app/default-handler
                       :controller empty/controller
                       :behavior   [posts-behaviors/get-map]}
              :put    {:handler    xiana.app/default-handler
                       :controller empty/controller
                       :behavior   [posts-behaviors/put-map]}
              :post   {:handler    xiana.app/default-handler
                       :controller empty/controller
                       :behavior   [posts-behaviors/post-map]}
              :delete {:handler    xiana.app/default-handler
                       :controller empty/controller
                       :behavior   [posts-behaviors/delete-map]}}]
   ["/posts/ids" {:post {:handler    xiana.app/default-handler
                         :controller empty/controller
                         :behavior   [posts-behaviors/multi-get-map]}}]
   ["/posts/comments" {:get {:handler    xiana.app/default-handler
                             :controller empty/controller
                             :behavior   [posts-behaviors/get-with-comments]}}]
   ["/comments" {:get    {:handler    xiana.app/default-handler
                          :controller empty/controller
                          :behavior   [comments-behaviors/get-map]}
                 :put    {:handler    xiana.app/default-handler
                          :controller empty/controller
                          :behavior   [comments-behaviors/put-map]}
                 :post   {:handler    xiana.app/default-handler
                          :controller empty/controller
                          :behavior   [comments-behaviors/post-map]}
                 :delete {:handler    xiana.app/default-handler
                          :controller empty/controller
                          :behavior   [comments-behaviors/delete-map]}}]
   ["/comments/ids" {:post {:handler    xiana.app/default-handler
                            :controller empty/controller
                            :behavior   [comments-behaviors/multi-get-map]}}]
   ["/assets/*" (ring/create-resource-handler)]])

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
                                  interceptors/params
                                  interceptors/require-logged-in
                                  interceptors/session-interceptor
                                  interceptors/view
                                  interceptors/db-access
                                  interceptors/acl-restrict
                                  interceptors/query-builder])
        ;interceptors/log])
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
