(ns router
  (:require
    [controllers.index :as index]
    [controllers.login :as login]
    [controllers.logout :as logout]
    [controllers.secret :as secret]
    [framework.interceptor.core :as x-interceptors]
    [framework.webserver.core :as ws]
    [interceptors :refer [inject-session?
                          logout
                          require-logged-in]]))

(def routes
  [["" {:handler ws/handler-fn}]
   ["/" {:action       index/index
         :interceptors [x-interceptors/params
                        inject-session?]}]
   ["/login" {:action login/login-controller}]
   ["/logout" {:action logout/logout-controller
               :interceptors {:around [logout]}}]
   ["/secret" {:action secret/protected-controller
               :interceptors {:inside [require-logged-in]}}]])
