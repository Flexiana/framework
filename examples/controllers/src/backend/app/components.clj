(ns components
  (:require
    [com.stuartsierra.component :as component]
    [framework.components.app.core :as xiana.app]
    [framework.components.router.core :as xiana.router]
    [framework.components.web-server.core :as xiana.web-server]
    [framework.config.core :as config]
    [framework.db.storage :as db.storage]
    [interceptors]
    [router]))

;TODO rename to something like main


(defn system
  [config]
  (let [pg-cfg (:framework.db.storage/postgresql config)
        app-cfg (:framework.app/ring config)
        web-server-cfg (:framework.app/web-server config)]
    (->
      (component/system-map
        :config config
        :db (db.storage/postgresql pg-cfg)
        :router (xiana.router/make-router router/routes)
        :app (xiana.app/make-app app-cfg
                                 [interceptors/wrap-path-params]
                                 [interceptors/require-logged-in])
        :web-server (xiana.web-server/make-web-server web-server-cfg))
      (component/system-using
        {:router     [:db]
         :app        [:router :db]
         :web-server [:app]}))))

(defn -main
  [& _args]
  (let [config (config/edn)]
    (component/start (system config))))
