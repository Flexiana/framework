(ns todoapp.core
  (:require
    [todoapp.controllers.index :as index]
    [framework.config.core :as config]
    [framework.db.core :as db-core]
    [framework.route.core :as routes]
    [framework.webserver.core :as ws]
    [jsonista.core :as json]
    [reitit.core :as r]
    [ring.util.response :as ring]
    [xiana.core :as xiana]))


;; (defn controller:hello
;;   [state]
;;   (-> (xiana/flow-> state
;;                     ((fn [state]
;;                        (let [conn (-> state :deps :db :connection)]
;;                          (-> state
;;                              (assoc :response {:body    (-> (jdbc/execute! conn
;;                                                                            ["SELECT * FROM todo"])
;;                                                             json/write-value-as-string)
;;                                                :headers {"Content-Type" "application/json"}
;;                                                :status  202})
;;                              xiana/ok)))))
;;       xiana/ok))

(def routes [["/" {:action index/handle-index}]
             #_["/hello" {:action controller:hello}]])

(defn system
  [config]
  (let [deps {:webserver        (:framework.app/web-server config)
              :routes           (routes/reset routes)
              :db               (db-core/start
                                 (:framework.db.storage/postgresql config))}]]
    (assoc deps :web-server (ws/start deps))))

(defn -main
  [& _args]
  (let [config (config/env)]
    (system config)))
