(ns state-events.core
  (:require
    [framework.config.core :as config]
    [framework.db.core :as db]
    [framework.interceptor.core :as interceptors]
    [framework.rbac.core :as rbac]
    [framework.route.core :as routes]
    [framework.session.core :as session]
    [framework.sse.core :as sse]
    [framework.webserver.core :as ws]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [reitit.ring :as ring]
    [state-events.controllers.index :as index]
    [state-events.controllers.person :as person]
    [state-events.interceptors.event-process :as events]
    [state-events.controllers.re-frame :as re-frame]
    [xiana.commons :refer [rename-key]]))

(def routes
  [["/" {:action index/handle-index}]
   ["/re-frame" {:action re-frame/handle-index}]
   ["/assets/*" (ring/create-resource-handler {:path "/"})]
   ["/person" {:put  {:action person/add}
               :post {:action person/modify}}]
   ["/sse" {:ws-action sse/sse-action}]])

(defn ->system
  [app-cfg]
  (-> (config/config app-cfg)
      (rename-key :framework.app/auth :auth)
      routes/reset
      rbac/init
      session/init-in-memory
      db/start
      db/migrate!
      sse/init
      ws/start
      closeable-map))

(def app-cfg
  {:routes                  routes
   :router-interceptors     []
   :controller-interceptors [(interceptors/muuntaja)
                             interceptors/params
                             session/guest-session-interceptor
                             interceptors/side-effect
                             events/interceptor
                             interceptors/view
                             db/db-access
                             rbac/interceptor]})

(defn -main
  [& _args]
  (->system app-cfg))
