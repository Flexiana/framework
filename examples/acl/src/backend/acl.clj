(ns acl
  (:require
    [clojure.data.xml :as xml]
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
    [muuntaja.format.json :as json-format]
    [nrepl.server :refer [start-server stop-server]]
    [reitit.ring :as ring]
    [reitit.ring.coercion :as rrc]
    [reitit.ring.middleware.muuntaja :as rm]
    [reitit.ring.middleware.parameters :as parameters]))

(defn xml-encoder
  [_options]
  (let [helper #(xml/emit-str
                  (mapv (fn make-node
                          [[f s]]
                          (if (map? s)
                            (xml/element f {} (map make-node (seq s)))
                            (xml/element f {} s)))
                    (seq %)))]
    (reify
      muuntaja.format.core/EncodeToBytes
      (encode-to-bytes [_ data charset]
        (.getBytes ^String (helper data) ^String charset)))))

(def minun-muuntajani
  (muuntaja.core/create
    (-> muuntaja.core/default-options
        (assoc-in [:formats "application/upper-json"]
          {:decoder [json-format/decoder]
           :encoder [json-format/encoder {:encode-key-fn (comp clojure.string/upper-case name)}]})
        (assoc-in [:formats "application/xml"] {:encoder [xml-encoder]})
        (assoc-in [:formats "application/json" :decoder-opts :bigdecimals] true)
        (assoc-in [:formats "application/json" :encoder-opts :date-format] "yyyy-MM-dd"))))

(def routes
  [["/" {:controller index/handle-index}]
   ["/re-frame" {:controller re-frame/handle-index}]
   ["" {:muuntaja   minun-muuntajani
        :handler    custom-handlers/post-handler
        :middleware [parameters/parameters-middleware
                     rm/format-middleware
                     rrc/coerce-request-middleware
                     rrc/coerce-response-middleware]}
    ["/posts" {:get    {;:handler    xiana.app/default-handler
                        :controller empty/controller
                        :behavior   [posts-behaviors/get-map]}
               :put    {;:handler    custom-handlers/post-handler
                        :controller empty/controller
                        :behavior   [posts-behaviors/put-map]}
               :post   {;:handler    xiana.app/default-handler
                        :controller empty/controller
                        :behavior   [posts-behaviors/post-map]}
               :delete {;:handler    xiana.app/default-handler
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
    ["/users" {:get    {:handler    xiana.app/default-handler
                        :controller empty/controller
                        :behavior   [users-behaviors/get-map]}
               :put    {:handler    xiana.app/default-handler
                        :controller empty/controller
                        :behavior   [users-behaviors/put-map]}
               :post   {:handler    xiana.app/default-handler
                        :controller empty/controller
                        :behavior   [users-behaviors/post-map]}
               :delete {:handler    xiana.app/default-handler
                        :controller empty/controller
                        :behavior   [users-behaviors/delete-map]}}]
    ["/users/ids" {:post {:handler    xiana.app/default-handler
                          :controller empty/controller
                          :behavior   [users-behaviors/multi-get-map]}}]
    ["/users/posts" {:get {:handler    xiana.app/default-handler
                           :controller empty/controller
                           :behavior   [users-behaviors/fetch-posts]}}]
    ["/users/posts/comments" {:get {:handler    xiana.app/default-handler
                                    :controller empty/controller
                                    :behavior   [users-behaviors/fetch-posts-comments]}}]]
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
                                 [parameters/parameters-middleware
                                  rm/format-middleware
                                  rrc/coerce-request-middleware
                                  rrc/coerce-response-middleware]
                                 [;interceptors/log
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
