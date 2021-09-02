(ns framework.router
  (:require
    [framework.users.endpoints :as users]
    [muuntaja.core :as m]
    [reitit.ring :as ring]
    [reitit.ring.middleware.muuntaja :as rm]))


(defn wrap-cors
  [handler]
  (fn [request]
    (update (handler request) :headers
            (fnil merge {}) {"Access-Control-Allow-Origin" "*"})))


(def routes
  [["/v1" users/endpoints]])


(def options
  {:data {:muuntaja m/instance
          :middleware [wrap-cors
                       rm/format-middleware]}})


(def api
  (ring/ring-handler
    (ring/router routes options)))
