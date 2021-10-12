(ns components
  (:require
    [framework.config.core :as config]
    [framework.interceptor.core :as xiana-interceptors]
    [framework.route.core :as routes]
    [framework.webserver.core :as ws]
    [interceptors]
    [router]))

(defn system
  [config]
  (let [deps {:webserver               (:framework.app/web-server config)
              :routes                  (routes/reset router/routes)
              :router-interceptors     []
              :controller-interceptors [(xiana-interceptors/muuntaja)
                                        xiana-interceptors/params
                                        interceptors/coerce
                                        interceptors/require-logged-in]}]
    (assoc deps :web-server (ws/start deps))))

(defn -main
  [& _args]
  (let [config (config/env)]
    (system config)))
