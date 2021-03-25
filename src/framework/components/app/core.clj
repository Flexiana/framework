(ns framework.components.app.core
  (:require
    [cats.core :as m]
    [com.stuartsierra.component :as component]
    [framework.acl.builder :as acl-builder]
    [reitit.core :as r]
    [xiana.commons :refer [?assoc-in]]
    [xiana.core :as xiana]))

(defn create-empty-state
  []
  (xiana/map->State {}))

(defn add-deps
  [state deps]
  (xiana/ok
    (assoc state :deps deps)))

(defn add-session-backend
  [state session-backend]
  (xiana/ok
    (update state :deps assoc :session-backend session-backend)))

(defn add-http-request
  [state http-request]
  (xiana/ok
    (assoc state :request http-request)))

(defn response
  [state response]
  (assoc state :response response))

(defn default-controller
  [{request :request {handler :handler} :request-data :as state}]
  (try
    (xiana/ok (assoc state :response (handler request)))
    (catch Exception e
      (xiana/error (-> state
                       (assoc :controller-error e)
                       (assoc :response {:status 500 :body "Internal Server error"}))))))

(defn route
  [{request :request {router :router} :deps :as state}]
  (let [match (r/match-by-path (:ring-router router) (:uri request))
        method (:request-method request)
        handler (or (get-in match [:data :handler]) (-> match :result method :handler))
        controller (get-in match [:data :controller])]
    (if controller
      (xiana/ok (-> state
                    (?assoc-in [:request-data :match] match)
                    (?assoc-in [:request-data :handler] handler)
                    (assoc-in [:request-data :controller] controller)))

      (if handler
        (xiana/ok (-> state
                      (?assoc-in [:request-data :match] match)
                      (assoc-in [:request-data :handler] handler)
                      (assoc-in [:request-data :controller] default-controller)))
        (xiana/error (response state {:status 404 :body "Not Found"}))))))

(defn run-controller
  [state]
  (let [controller (get-in state [:request-data :controller])]
    (controller state)))

(defn select-interceptors
  [interceptors context ordering]
  (->> interceptors
       (filter context)
       ordering
       (map context)
       (map #(fn [x] (% x)))))

(defn init-acl
  [this config]
  (acl-builder/init this config))

(defrecord App
  [config acl-cfg session-backend router db]
  component/Lifecycle
  (stop [this] this)
  (start [this]
    (assoc this
      :handler
      (fn [http-request]
        (->
          (apply m/>>=
                 (concat
                   [(xiana.core/ok (create-empty-state))
                    (fn [x] (add-deps x {:router router, :db db}))
                    (fn [x] (add-session-backend x session-backend))
                    (fn [x] (add-http-request x http-request))
                    (fn [x] (init-acl x acl-cfg))]
                   (-> this :router-interceptors (select-interceptors :enter identity))
                   [(fn [x] (route x))]
                   (-> this :router-interceptors (select-interceptors :leave reverse))
                   (-> this :controller-interceptors (select-interceptors :enter identity))
                   [(fn [x] (run-controller x))]
                   (-> this :controller-interceptors (select-interceptors :leave reverse))))
          (xiana/extract)
          (get :response))))))

(defn make-app
  [config acl-cfg session-backend router-interceptors controller-interceptors]
  (map->App {:config                  config
             :acl-cfg                 acl-cfg
             :session-backend         session-backend
             :router-interceptors     router-interceptors
             :controller-interceptors controller-interceptors}))
