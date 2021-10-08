(ns acl
  (:require
    [controllers.comments :as comments-controllers]
    [controllers.index :as index]
    [controllers.posts :as posts-controllers]
    [controllers.re-frame :as re-frame]
    [controllers.users :as users-controllers]
    [framework.config.core :as config]
    [framework.db.core :as db-core]
    [framework.interceptor.core :as interceptors]
    [framework.rbac.core :as rbac]
    [framework.route.core :as routes]
    [framework.session.core :as session]
    [framework.webserver.core :as ws]
    [interceptors.load-user :as user]
    [nrepl.server :refer [start-server]]
    [reitit.ring :as ring]))

(def routes
  [["/" {:action index/handle-index}]
   ["/re-frame" {:action re-frame/handle-index}]
   ["/assets/*" (ring/create-resource-handler)]
   ["" {:handler ws/handler-fn}
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

(defonce server (start-server :port 7888))

(defn system
  [config]
  (let [session-backend (:session-backend config (session/init-in-memory))
        deps {:webserver               (:framework.app/web-server config)
              :routes                  (routes/reset routes)
              :role-set                (rbac/init (:framework.app/role-set config))
              :auth                    (:framework.app/auth config)
              :session-backend         session-backend
              :router-interceptors     [(interceptors/message "router ---------------")]
              :controller-interceptors [(interceptors/message 0)
                                        (interceptors/muuntaja)
                                        (interceptors/message 1)
                                        interceptors/params
                                        (interceptors/message 2)
                                        ;(interceptors/session-user-id session-backend)
                                        user/login-user
                                        (interceptors/message 3)
                                        ;(interceptors/session-user-role)
                                        (interceptors/message 4)
                                        ;(session/interceptor session-backend)
                                        (interceptors/message 5)
                                        interceptors/view
                                        (interceptors/message 6)
                                        interceptors/db-access
                                        (interceptors/message 7)
                                        rbac/interceptor
                                        (interceptors/message 8)]
              :db                      (db-core/start
                                         (:framework.db.storage/postgresql config))}]
    (assoc deps :web-server (ws/start deps))))

(defn -main
  [& _args]
  (let [config (config/env)]
    (system config)))
