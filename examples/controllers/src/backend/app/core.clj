(ns core
  (:require
    [framework.coercion.core :as coercion]
    [framework.config.core :as config]
    [framework.interceptor.core :as xiana-interceptors]
    [framework.route.core :as routes]
    [framework.webserver.core :as ws]
    [interceptors]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [route]))

(defn ->system
  [app-cfg]
  (-> (config/config)
      (merge app-cfg)
      routes/reset
      ws/start
      closeable-map))

(def app-cfg
  {:routes                  route/routes
   :controller-interceptors [(xiana-interceptors/muuntaja)
                             xiana-interceptors/params
                             coercion/interceptor
                             interceptors/require-logged-in]})

(defn -main
  [& _args]
  (->system app-cfg))
