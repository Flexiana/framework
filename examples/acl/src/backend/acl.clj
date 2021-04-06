(ns acl
  (:require
    [com.stuartsierra.component :as component]
    [controller-behaviors.comments :as comments-behaviors]
    [controller-behaviors.posts :as posts-behaviors]
    [controller-behaviors.users :as users-behaviors]
    [controllers.index :as index]
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
    ["/posts" {:get    {:controller empty/controller
                        :behavior   [posts-behaviors/get-map]}
               :put    {:controller empty/controller
                        :behavior   [posts-behaviors/put-map]}
               :post   {:controller empty/controller
                        :behavior   [posts-behaviors/post-map]}
               :delete {:controller empty/controller
                        :behavior   [posts-behaviors/delete-map]}}]
    ["/posts/ids" {:post {:controller empty/controller
                          :behavior   [posts-behaviors/multi-get-map]}}]
    ["/posts/comments" {:get {:controller empty/controller
                              :behavior   [posts-behaviors/get-with-comments]}}]
    ["/comments" {:get    {:controller empty/controller
                           :behavior   [comments-behaviors/get-map]}
                  :put    {:controller empty/controller
                           :behavior   [comments-behaviors/put-map]}
                  :post   {:controller empty/controller
                           :behavior   [comments-behaviors/post-map]}
                  :delete {:controller empty/controller
                           :behavior   [comments-behaviors/delete-map]}}]
    ["/comments/ids" {:post {:controller empty/controller
                             :behavior   [comments-behaviors/multi-get-map]}}]
    ["/users" {:get    {:controller empty/controller
                        :behavior   [users-behaviors/get-map]}
               :put    {:controller empty/controller
                        :behavior   [users-behaviors/put-map]}
               :post   {:controller empty/controller
                        :behavior   [users-behaviors/post-map]}
               :delete {:controller empty/controller
                        :behavior   [users-behaviors/delete-map]}}]
    ["/users/ids" {:post {:controller empty/controller
                          :behavior   [users-behaviors/multi-get-map]}}]
    ["/users/posts" {:get {:controller empty/controller
                           :behavior   [users-behaviors/fetch-posts]}}]
    ["/users/posts/comments" {:get {:controller empty/controller
                                    :behavior   [users-behaviors/fetch-posts-comments]}}]]])

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
                                  interceptors/acl-restrict
                                  interceptors/query-builder])
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
