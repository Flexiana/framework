(ns state-events.core
  (:require
    [clojure.core.async :as async]
    [framework.config.core :as config]
    [framework.cookies.core :as cookies]
    [framework.db.core :as db]
    [framework.interceptor.core :as interceptors]
    [framework.rbac.core :as rbac]
    [framework.route.core :as routes]
    [framework.scheduler.core :as scheduler]
    [framework.session.core :as session]
    [framework.sse.core :as sse]
    [framework.webserver.core :as ws]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [reitit.ring :as ring]
    [state-events.controllers.event :as event]
    [state-events.controllers.index :as index]
    [state-events.controllers.re-frame :as re-frame]
    [state-events.interceptors :refer [asset-router
                                       session-id->cookie]]
    [state-events.interceptors.event-process :as events]
    [xiana.commons :refer [rename-key]]
    [xiana.core :as xiana])
  (:import
    (java.util
      Date)))

(defn resource-handler [state]
  (let [f (ring/create-resource-handler {:path "/"})]
    (prn (get-in state [:request :uri]))
    (xiana/ok (assoc state :response (f (:request state))))))

(def event-interceptors
  [(interceptors/muuntaja)
   interceptors/params
   (interceptors/keyceptor :response)
   cookies/interceptor
   session-id->cookie
   session/guest-session-interceptor
   interceptors/view
   interceptors/side-effect
   events/interceptor
   db/db-access])

(def default-interceptors
  [cookies/interceptor
   (interceptors/muuntaja)
   interceptors/params
   session-id->cookie
   session/guest-session-interceptor
   interceptors/view
   interceptors/side-effect
   db/db-access])

(def routes
  [["/" {:action index/handle-index}]
   ["/re-frame" {:action re-frame/handle-index}]
   ["/assets/*" {:action resource-handler}]
   ["/person" {:put  {:action       event/create-resource
                      :interceptors event-interceptors}
               :post {:action       event/modify
                      :interceptors event-interceptors}}]
   ["/events" {:get {:action event/collect}}]
   ["/sse" {:ws-action sse/sse-action}]])

(defn docker?
  [state]
  (if (get-in state [:framework.db.storage/postgresql :image-name])
    (db/docker-postgres! state)
    state))

(defonce ^{:private true} ping-id (atom 0))

(defn ping [deps]
  (let [channel (get-in deps [:events-channel :channel])]
    (async/>!! channel {:type :ping :id (swap! ping-id inc) :timestamp (.getTime (Date.))})))

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
      (scheduler/start ping 10000)
      ws/start
      closeable-map))

(def app-cfg
  {:routes                  routes
   :router-interceptors     [asset-router]
   :controller-interceptors default-interceptors})

(defn -main
  [& _args]
  (->system app-cfg))
