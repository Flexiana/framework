(ns app.core
  (:require
    [app.controllers.index :as index]
    [app.controllers.login :as login]
    [app.controllers.secret :as secret]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [xiana.config :as config]
    [xiana.handler :as x-handler]
    [xiana.interceptor :as x-interceptors]
    [xiana.interceptor.error]
    [xiana.jwt :as jwt]
    [xiana.jwt.interceptors :as jwt-interceptors]
    [xiana.route :as x-routes]
    [xiana.webserver :as ws]))

(def routes
  [["" {:handler x-handler/handler-fn}]
   ["/" {:action index/index}]
   ["/login" {:post
              {:action       login/login-controller
               :interceptors {:except [jwt-interceptors/jwt-auth]}}}]
   ["/secret" {:post
               {:action secret/protected-controller}}]])

(defn ->system
  [app-cfg]
  (-> (config/config app-cfg)
      jwt/init-from-file
      x-routes/reset
      ws/start
      closeable-map))

(def app-cfg
  {:routes                  routes
   :controller-interceptors [(x-interceptors/muuntaja)
                             xiana.interceptor.error/response
                             x-interceptors/params
                             jwt-interceptors/jwt-auth]})

(defn -main
  [& _args]
  (->system app-cfg))
