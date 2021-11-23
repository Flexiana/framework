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
    [state-events.controllers.event :as event]
    [state-events.controllers.index :as index]
    [state-events.controllers.re-frame :as re-frame]
    [state-events.interceptors.event-process :as events]
    [xiana.commons :refer [rename-key]]
    [xiana.core :as xiana]))

(defn resource-handler [state]
  (let [f (ring/create-resource-handler  {:path "/"})]
    (prn (get-in state [:request :uri]))
    (xiana/ok (assoc state :response (f (:request state))))))

(def routes
  [["/" {:action       index/handle-index
         :interceptors {:except [events/interceptor]}}]
   ["/re-frame" {:action re-frame/handle-index
                 :interceptors {:except [events/interceptor]}}]
   ["/assets/*" {:action resource-handler
                 :interceptors {:except [events/interceptor]}}]
   ["/person" {:put  {:action event/add}
               :post {:action event/modify}}]
   ["/sse" {:ws-action sse/sse-action
            :interceptors {:except [events/interceptor]}}]])

(defn docker?
  [state]
  (if (get-in state [:framework.db.storage/postgresql :image-name])
    (db/docker-postgres! state)
    state))

(def asset-router
  {:enter (fn [{{uri :uri} :request
                :as        state}]
            (xiana/ok
              (case uri
                "/favicon.ico" (assoc-in state [:request :uri] "/assets/favicon.ico")
                state)))})

(defn ->system
  [app-cfg]
  (-> (config/config app-cfg)
      (rename-key :framework.app/auth :auth)
      routes/reset
      rbac/init
      session/init-in-memory
      docker?
      db/connect
      db/migrate!
      sse/init
      ws/start
      closeable-map))

(def app-cfg
  {:routes                  routes
   :router-interceptors     [asset-router]
   :controller-interceptors [(interceptors/muuntaja)
                             interceptors/params
                             session/guest-session-interceptor
                             interceptors/view
                             interceptors/side-effect
                             events/interceptor
                             db/db-access]})

(defn -main
  [& _args]
  (->system app-cfg))
