(ns state-events.core
  (:require
    [clj-test-containers.core :as tc]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [reitit.ring :as ring]
    [state-events.controller-behaviors.sse :as sseb]
    [state-events.controllers.event :as event]
    [state-events.controllers.index :as index]
    [state-events.controllers.re-frame :as re-frame]
    [state-events.interceptors :refer [asset-router
                                       session-id->cookie]]
    [state-events.interceptors.event-process :as events]
    [xiana.commons :refer [rename-key]]
    [xiana.config :as config]
    [xiana.cookies :as cookies]
    [xiana.db :as db]
    [xiana.interceptor :as interceptors]
    [xiana.rbac :as rbac]
    [xiana.route :as routes]
    [xiana.scheduler :as scheduler]
    [xiana.session :as session]
    [xiana.sse :as sse]
    [xiana.webserver :as ws]))

(defn resource-handler [state]
  (let [f (ring/create-resource-handler {:path "/"})]
    (assoc state :response (f (:request state)))))

(def event-interceptors
  [(interceptors/muuntaja)
   interceptors/params
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
   ["/person" {:put    {:action       event/create-resource
                        :interceptors event-interceptors}
               :post   {:action       event/modify
                        :interceptors event-interceptors}
               :delete {:action       event/delete
                        :interceptors event-interceptors}
               :get    {:action event/persons}}]
   ["/events" {:get {:action event/raw}}]
   ["/sse" {:ws-action sse/sse-action}]])

(defn docker-postgres!
  [{pg-config :xiana/postgresql :as config}]
  (let [{:keys [dbname user password image-name]} pg-config
        container (tc/start!
                    (tc/create
                      {:image-name    image-name
                       :exposed-ports [5432]
                       :env-vars      {"POSTGRES_DB"       dbname
                                       "POSTGRES_USER"     user
                                       "POSTGRES_PASSWORD" password}}))

        port (get (:mapped-ports container) 5432)
        pg-config (assoc
                    pg-config
                    :port port
                    :embedded container
                    :subname (str "//localhost:" port "/" dbname))]
    (tc/wait {:wait-strategy :log
              :message       "accept connections"} (:container container))
    (assoc config :xiana/postgresql pg-config)))

(defn docker?
  [state]
  (if (get-in state [:xiana/postgresql :image-name])
    (docker-postgres! state)
    state))

(defn ->system
  [app-cfg]
  (-> (config/config app-cfg)
      (rename-key :xiana/auth :auth)
      routes/reset
      rbac/init
      session/init-backend
      docker?
      db/connect
      db/migrate!
      sse/init
      (scheduler/start sseb/ping 10000)
      ws/start
      closeable-map))

(def app-cfg
  {:routes                  routes
   :router-interceptors     [asset-router]
   :controller-interceptors default-interceptors})

(defn -main
  [& _args]
  (->system app-cfg))
