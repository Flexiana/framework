(ns components
  (:require
    [app]
    [framework.config.core :as config]
    [framework.interceptor.core :as interceptor]
    [framework.route.core :as routes]
    [framework.webserver.core :as ws]))

(defn deps
  [config]
  {:routes (routes/reset app/routes)
   :webserver (:framework.app/web-server config)
   :controller-interceptors [(interceptor/muuntaja)]})

(defn -main
  [& args]
  (let [config (config/env)]
    (ws/start (deps config))))
