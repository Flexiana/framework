(ns app.core
  (:require
    [app.config :refer [jwt-config]]
    [app.controllers.index :as index]
    [app.controllers.login :as login]
    [app.controllers.secret :as secret]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [xiana.config :as config]
    [xiana.handler :as x-handler]
    [xiana.interceptor :as x-interceptors]
    [xiana.interceptor.error]
    [xiana.route :as x-routes]
    [xiana.webserver :as ws]))

(def routes
  [["" {:handler x-handler/handler-fn}]
   ["/" {:action index/index}]
   ["/login" {:post
              {:action       login/login-controller
               :interceptors {:except [x-interceptors/jwt-auth]}}}]
   ["/secret" {:post
               {:action secret/protected-controller}}]])

(defn ->system
  [app-cfg]
  (-> (config/config)
      (merge jwt-config)
      (merge app-cfg)
      x-routes/reset
      ws/start
      closeable-map))

(def app-cfg
  {:routes                  routes
   :controller-interceptors [(x-interceptors/muuntaja)
                             xiana.interceptor.error/response
                             x-interceptors/params
                             x-interceptors/jwt-auth]})

(defn -main
  [& _args]
  (->system app-cfg))
