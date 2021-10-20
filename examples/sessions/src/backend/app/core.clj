(ns app.core
  (:require
    [app.router :as router]
    [framework.config.core :as x-config]
    [framework.interceptor.core :as x-interceptors]
    [framework.route.core :as x-routes]
    [framework.session.core :as x-session]
    [framework.webserver.core :as x-ws]))

(defn system
  [config]
  (let [deps {:webserver               (:framework.app/web-server config)
              :routes                  (x-routes/reset router/routes)
              :session-backend         (x-session/init-in-memory)
              :controller-interceptors [x-interceptors/params
                                        (x-session/interceptor "" "/login")]}]
    (x-ws/start deps)))

(defn -main
  [& _args]
  (let [config (x-config/env)]
    (system config)))
