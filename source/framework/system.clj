(ns framework.system
  (:require
    [framework.router :as router]
    [environ.core :refer [env]]
    [integrant.core :as ig]
    [ring.adapter.jetty :as jetty]))


(def model
  {:api/server {:port 5000
                :jdbc-url (ig/ref :api/database)}
   :api/database {:jdbc-url (str "jdbc:postgresql://localhost:5432"
                                 "/framework-db"
                                 "?user=brahayan"
                                 "&password=development")}})


(defmethod ig/init-key :api/server
  [_ {:keys [port]}]
  (jetty/run-jetty router/api {:port port :join? false}))


(defmethod ig/prep-key :api/server
  [_ base]
  (let [port? (-> (env :port) nil? not)]
    (if port?
      (merge base {:port (Integer. (env :port))}) base)))


(defmethod ig/halt-key! :api/server
  [_ jetty]
  (.stop jetty))


(defmethod ig/init-key :api/database
  [_ base]
  (:jdbc-url base))


(defmethod ig/prep-key :api/database
  [_ base]
  (let [jdbc-url? (-> (env :jdbc-database-url) nil? not)]
    (if jdbc-url?
      (merge base {:jdbc-url (env :jdbc-database-url)}) base)))
