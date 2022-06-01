(ns core
  (:require
    [interceptors]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [route]
    [xiana.coercion :as coercion]
    [xiana.config :as config]
    [xiana.interceptor :as xiana-interceptors]
    [xiana.interceptor.error]
    [xiana.route :as routes]
    [xiana.webserver :as ws]))

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
                             xiana.interceptor.error/response
                             interceptors/require-logged-in
                             xiana-interceptors/params
                             coercion/interceptor]})

(defn -main
  [& _args]
  (->system app-cfg))
