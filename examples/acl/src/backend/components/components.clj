(ns components
  (:require [app]
            [web-server]
            [router]
            [com.stuartsierra.component :as component]
            [framework.config.core :as config]
            [framework.db.storage :as db.storage]))

(defn system
  [config]
  (let [pg-cfg (:framework.db.storage/postgresql config)
        app-cfg (:framework.app/ring config)
        web-server-cfg (:framework.app/web-server config)
        acl-permissions (:acl/permissions config)]
    (->
      (component/system-map
        :config config
        :db (db.storage/postgresql pg-cfg)
        :router (router/make-router)
        :app (app/make-app app-cfg)
        :acl-permissions acl-permissions
        :web-server (web-server/make-web-server web-server-cfg))
      (component/system-using
        {:router     [:db]
         :app        [:router :db :acl-permissions]
         :web-server [:app]}))))

(defn -main
  [& _args]
  (let [config (config/edn)]
    (component/start (system config))))