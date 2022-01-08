(ns cli-chat.core
  (:require
    [cli-chat.controllers.chat :refer [chat-action]]
    [cli-chat.controllers.index :as index]
    [cli-chat.controllers.re-frame :as re-frame]
    [framework.config.core :as config]
    [framework.db.core :as db]
    [framework.interceptor.core :as interceptors]
    [framework.rbac.core :as rbac]
    [framework.route.core :as routes]
    [framework.session.core :as session]
    [framework.webserver.core :as ws]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [reitit.ring :as ring]
    [xiana.commons :refer [rename-key]]))

(def routes
  [["/" {:action index/handle-index}]
   ["/re-frame" {:action re-frame/handle-index}]
   ["/assets/*" (ring/create-resource-handler {:path "/"})]
   ["/chat" {:ws-action chat-action}]])

(defn ->system
  [app-cfg]
  (-> (config/config)
      (merge app-cfg)
      routes/reset
      db/connect
      db/migrate!
      rbac/init
      (rename-key :framework.app/auth :auth)
      session/init-backend
      ws/start
      closeable-map))

(def app-cfg
  {:routes                  routes
   :router-interceptors     []
   :web-socket-interceptors [interceptors/params
                             session/guest-session-interceptor]
   :controller-interceptors [(interceptors/muuntaja)
                             interceptors/params
                             session/guest-session-interceptor
                             interceptors/view
                             interceptors/side-effect
                             db/db-access
                             rbac/interceptor]})

(defn -main
  [& _args]
  (->system app-cfg))

