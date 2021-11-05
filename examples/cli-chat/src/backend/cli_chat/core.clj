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
    [reitit.ring :as ring]))

(def routes
  [["/" {:action index/handle-index}]
   ["/re-frame" {:action re-frame/handle-index}]
   ["/assets/*" (ring/create-resource-handler {:path "/"})]
   ["/chat" {:ws-action chat-action}]])

(defn system
  [config]
  (let [deps {:routes                  (routes/reset routes)
              :db                      (db/start (:framework.db.storage/postgresql config))
              :webserver               (:framework.app/web-server config)
              :role-set                (rbac/init (:framework.app/role-set config))
              :auth                    (:framework.app/auth config)
              :session-backend         (session/init-in-memory)
              :router-interceptors     []
              :web-socket-interceptors [interceptors/params
                                        session/guest-session-interceptor]
              :controller-interceptors [(interceptors/muuntaja)
                                        interceptors/params
                                        session/guest-session-interceptor
                                        interceptors/view
                                        interceptors/side-effect
                                        db/db-access
                                        rbac/interceptor]}]
    (assoc deps :webserver (ws/start deps))))

(defonce system-map (atom {}))

(defn -main
  [& _args]
  {:pre (even? (count _args))}
  (reset! system-map (-> (config/env)
                         (merge _args)
                         system)))

(comment (-main))
