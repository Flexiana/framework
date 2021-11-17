(ns state-events.core
  (:require
    [state-events.controllers.index :as index]
    [state-events.controllers.re-frame :as re-frame]
    [state-events.controllers.person :as person]
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
   ["/person" {:get {:action person/fetch}
               :put {:action person/add}
               :post {:action person/modify}}]])

(defn ->system
  [app-cfg]
  (-> (config/config app-cfg)
      (rename-key :framework.app/auth :auth)
      routes/reset
      rbac/init
      session/init-in-memory
      ;db/start
      ;db/migrate!
      ws/start
      closeable-map))

(def app-cfg
  {:routes routes
   :router-interceptors     []
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
