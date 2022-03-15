(ns acl
  (:require
    [controllers.comments :as comments-controllers]
    [controllers.index :as index]
    [controllers.posts :as posts-controllers]
    [controllers.re-frame :as re-frame]
    [controllers.users :as users-controllers]
    [framework.config.core :as config]
    [framework.db.core :as db]
    [framework.handler.core :as handler]
    [framework.interceptor.core :as interceptors]
    [framework.interceptor.error]
    [framework.rbac.core :as rbac]
    [framework.route.core :as routes]
    [framework.webserver.core :as ws]
    [interceptors.load-user :as user]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [reitit.ring :as ring]))

(def routes
  [["/" {:action index/handle-index}]
   ["/re-frame" {:action re-frame/handle-index}]
   ["/assets/*" (ring/create-resource-handler)]
   ["" {:handler handler/handler-fn}
    ["/posts" {:get    {:action     posts-controllers/fetch
                        :permission :posts/read}
               :put    {:action     posts-controllers/add
                        :permission :posts/create}
               :post   {:action     posts-controllers/update-post
                        :permission :posts/update}
               :delete {:action     posts-controllers/delete-post
                        :permission :posts/delete}}]
    ["/posts/ids" {:post {:action     posts-controllers/fetch-by-ids
                          :permission :posts/read}}]
    ["/posts/comments" {:get {:action     posts-controllers/fetch-with-comments
                              :permission :posts/read}}]
    ["/comments" {:get    {:action     comments-controllers/fetch
                           :permission :comments/read}
                  :put    {:action     comments-controllers/add
                           :permission :comments/create}
                  :post   {:action     comments-controllers/update-comment
                           :permission :comments/update}
                  :delete {:action     comments-controllers/delete-comment
                           :permission :comments/delete}}]
    ["/users" {:get    {:action     users-controllers/fetch
                        :permission :users/read}
               :put    {:action     users-controllers/add
                        :permission :users/create}
               :post   {:action     users-controllers/update-user
                        :permission :users/update}
               :delete {:action     users-controllers/delete-user
                        :permission :users/delete}}]
    ["/users/posts" {:get {:action     users-controllers/fetch-with-posts
                           :permission :users/read}}]
    ["/users/posts/comments" {:get {:action     users-controllers/fetch-with-posts-comments
                                    :permission :users/read}}]]])

(defn ->system
  [app-config]
  (-> (config/config)
      (merge app-config)
      db/connect
      db/migrate!
      routes/reset
      rbac/init
      ws/start
      closeable-map))

(def app-cfg
  {:routes                  routes
   :controller-interceptors [(interceptors/muuntaja)
                             framework.interceptor.error/handle-ex-info
                             interceptors/params
                             user/load-user!
                             interceptors/view
                             db/db-access
                             rbac/interceptor]})

(defn -main
  [& _args]
  (->system app-cfg))
