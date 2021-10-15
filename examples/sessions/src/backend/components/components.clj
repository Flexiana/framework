(ns components
  (:require
    [framework.config.core :as config]
    [framework.interceptor.core :as x-interceptors]
    [framework.route.core :as x-routes]
    [framework.session.core :as session]
    [framework.webserver.core :as ws]
    [router]))

(defn system
  [config]
  (let [deps {:webserver               (:framework.app/web-server config)
              :routes                  (x-routes/reset router/routes)
              :session-backend         (session/init-in-memory)
              :controller-interceptors [x-interceptors/params
                                        (session/interceptor "" "/login")]}]
    (ws/start deps)))

(defn -main
  [& _args]
  (let [config (config/env)]
    (system config)))
