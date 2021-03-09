(ns app
  (:require
    [com.stuartsierra.component :as component]
    [reitit.core :as r]
    [reitit.coercion :as rc]
    [reitit.ring :as ring]
    [ring.middleware.params :as params]
    [xiana.core :as xiana]
    [commons :refer :all]))

(defn create-empty-state
  []
  (xiana/map->State {}))

(defn add-deps
  [state deps]
  (xiana/ok
    (assoc state :deps deps)))

(defn add-http-request
  [state http-request]
  (xiana/ok
    (assoc state :http-request http-request)))

(defn response
  [state response]
  (assoc state :response response))

(defn route
  [{request :http-request {router :router} :deps :as state}]
  (let [match (r/match-by-path (:ring-router router) (:uri request))
        method (:request-method request)
        handler (or (get-in match [:data :handler]) (-> match :result method :handler))
        controller (get-in match [:data :controller])]
    (if controller
      (xiana/ok (-> state
                    (?assoc-in [:http-request :path-params] (:path-params match)) ;TODO middleware?
                    (?assoc-in [:request-data :match] match)
                    (?assoc-in [:request-data :handler] handler)
                    (assoc-in [:request-data :controller] controller)))

      (if handler
        ;; short-circuit
        ;; TODO: refactor it
        (let [resp (-> router (get :ring-router) (ring/ring-handler) (apply [request]))]
          (xiana/error (response state resp)))
        (xiana/error (response state {:status 404 :body "Not Found"}))))))

(defn pre-route-middlewares
  [state]
  (xiana/ok state))

(defn pre-controller-middlewares
  [state]
  (xiana/ok state))

(defn post-controller-middlewares
  [state]
  (xiana/ok state))

(defn run-controller
  [state]
  (let [controller (get-in state [:request-data :controller])]
    (controller state)))

(defrecord App
  [config router db]

  component/Lifecycle
  (stop [this] this)
  (start [this]
         (assoc this
           :handler
           (fn [http-request]
             (->
               (xiana/flow->
                 (create-empty-state)
                 (add-deps {:router router :db db})
                 (add-http-request http-request)
                 (pre-route-middlewares)
                 (route)
                 (pre-controller-middlewares)
                 (run-controller)
                 (post-controller-middlewares))
               (xiana/extract)
               (get :response))))))

(defn make-app
  [config]
  (map->App {:config config}))
