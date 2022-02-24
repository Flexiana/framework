(ns app.core
  (:require
    [app.controllers.index :as index]
    [app.controllers.login :as login]
    [app.controllers.logout :as logout]
    [app.controllers.secret :as secret]
    [app.interceptors :refer [inject-session?
                              logout
                              require-logged-in]]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [xiana.config :as config]
    [xiana.handler :as handler]
    [xiana.interceptor :as interceptors]
    [xiana.route :as route]
    [xiana.session :as session]
    [xiana.webserver :as ws]))

(def routes
  [["" {:handler handler/handler-fn}]
   ["/" {:action       index/index
         :interceptors [interceptors/params
                        inject-session?]}]
   ["/login" {:action login/login-controller
              :interceptors {:except [session/interceptor]}}]
   ["/logout" {:action       logout/logout-controller
               :interceptors {:around [logout]}}]
   ["/secret" {:action       secret/protected-controller
               :interceptors {:inside [require-logged-in]}}]])

(defn ->system
  [app-cfg]
  (-> (config/config)
      (merge app-cfg)
      route/reset
      session/init-backend
      ws/start
      closeable-map))

(def app-cfg
  {:routes                  routes
   :controller-interceptors [interceptors/params
                             session/interceptor]})

(defn -main
  [& _args]
  (->system app-cfg))
