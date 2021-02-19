(ns todoapp.core
  (:require [com.stuartsierra.component :as component]
            [framework.db.storage :as db.storage]
            [reitit.core :as r]
            [jsonista.core :as json]
            [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty]
            [xiana.core :as xiana]
            [next.jdbc :as jdbc]
            [config.core :refer (load-env)])
  (:import (org.eclipse.jetty.server Server)))
(defn make-web-server
  [web-server]
  (-> web-server
      (assoc :requires [{:app [:handler]}]
             :provides [:http-server])
      (with-meta `{component/stop  ~(fn [{:keys [^Server http-server]
                                         :as   this}]
                                      (.stop http-server)
                                      (dissoc this :http-server))
                   component/start ~(fn [{:keys [app]
                                         :as   this}]
                                      (assoc this :http-server (jetty/run-jetty (:handler app) this)))})))

(defn add-deps
  [state deps]
  (xiana/ok (assoc state :deps deps)))

(defn add-http-request
  [state http-request]
  (xiana/ok (assoc state :http-request http-request)))

(defn route
  [{:keys [http-request deps]
    :as   state}]
  (let [ring-router                  (-> deps :router :ring-router)
        {:keys [uri]}                http-request
        {:keys [handler controller]} (-> ring-router
                                         (r/match-by-path uri)
                                         :data)]
    (cond  controller (-> state
                          (assoc-in [:request-data :controller] controller)
                          xiana/ok)
           handler    (-> ring-router
                          ring/ring-handler
                          (apply [http-request])
                          (->> (assoc state :response))
                          xiana/error)
           :else      (->> {:status 404 :body "Not Found"}
                           (assoc state :response)
                           xiana/error))))

(defn run-controller
  [{:keys [request-data]
    :as   state}]
  (-> request-data
      :controller
      (apply [state])))
(defn state->handler [{:keys [router db]}
                      state
                      http-request]
  (fn [http-request]
    (-> (xiana/flow-> state
                      (add-deps {:router router
                                 :db     db})
                      (add-http-request http-request)
                      route
                      run-controller
                      (xiana/extract))
        :response)))

(defn make-app
  [ring]
  (-> ring
      (assoc :requires [:router :db]
             :provides [:handler])
      (with-meta `{component/stop  ~(fn [this] (dissoc this :handler))
                   component/start ~(fn [this]
                                      (assoc this
                                             :handler
                                             (partial state->handler this (xiana/map->State {}))))})))

(defn controller:index-view
  [state]
  (-> (xiana/flow-> state
                    ((fn [state]
                       (-> state
                           (assoc :response {:status  200
                                             :headers {"Content-Type" "text/plain"}
                                             :body    "Index page"})
                           xiana/ok))))
      (xiana/ok)))

(defn controller:hello
  [state]
  (-> (xiana/flow-> state
                    ((fn [state]
                       (let [conn (-> state :deps :db :connection)]
                         (-> state
                             (assoc :response {:body    (-> (jdbc/execute! conn
                                                                           ["SELECT * FROM todo"])
                                                            json/write-value-as-string)
                                               :headers {"Content-Type" "application/json"}
                                               :status  202})
                             xiana/ok)))))
      xiana/ok))


(defn make-router
  []
  (let [routes [["/" {:controller controller:index-view}]
                ["/hello" {:controller controller:hello}]]]
    (-> {:provides [:ring-router]}
        (with-meta `{component/stop  ~(fn [this]
                                        (dissoc this :ring-router))
                     component/start ~(fn [this]
                                        (assoc this :ring-router (ring/router routes)))}))))

(defn system
  [{:framework.db.storage/keys [postgresql]
    :framework.app/keys        [web-server ring]}]
  (let [system         (component/system-map :db (db.storage/postgresql postgresql)
                                             :router (make-router)
                                             :app (make-app ring)
                                             :web-server (make-web-server web-server))
        dependency-map {:app        [:router :db]
                        :web-server [:app]}]
    (component/system-using system dependency-map)))

(defn -main
  [& _args]
  (let [env (load-env)]
    (component/start (system env))))
