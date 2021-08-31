(ns conway.system
  (:require
    [conway.router :as router]
    [environ.core :refer [env]]
    [integrant.core :as ig]
    [ring.adapter.jetty :as jetty]))


(def model {:server/jetty {:port 5000}})


(defmethod ig/init-key :server/jetty
  [_ {:keys [port]}]
  (jetty/run-jetty router/api {:port port :join? false}))


(defmethod ig/prep-key :server/jetty
  [_ base]
  (let [port? (-> (env :port) nil? not)]
    (if port?
      (merge base {:port (Integer. (env :port))}) base)))


(defmethod ig/halt-key! :server/jetty
  [_ jetty]
  (.stop jetty))
