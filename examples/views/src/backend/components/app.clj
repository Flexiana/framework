(ns app
  (:require
    [com.stuartsierra.component :as component]
    [controllers.home-ctrl :as hct]
    [middlewares.core :as xmid]
    [reitit.core :as r]
    [reitit.ring :as ring]
    [view.core :as xview]
    [xiana.core :as xiana]))

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
        handler (get-in match [:data :handler])
        params (get match :path-params)
        controller (get-in match [:data :controller])]
    (if controller
      (-> state
          (assoc-in [:request-data :controller] controller)
          (assoc-in [:request-data :params] params)
          xiana/ok)
      (if handler
        ;; short-circuit
        ;; TODO: refactor it
        (let [resp (-> router (get :ring-router) (ring/ring-handler) (apply [request]))]
          (xiana/error (response state resp)))
        (xiana/error (response state {:status 404 :body "Not Found"}))))))

(defn run-controller
  [state]
  (let [controller (get-in state [:request-data :controller])]
    (controller state)))

(def create-handler)

(defrecord App [config router db]
  component/Lifecycle
  (start [this]
         (assoc this
           :handler
           (fn [http-request]
             (->
               (xiana/flow->
                 (create-empty-state)
                 (add-deps {:router router :db db})
                 (add-http-request http-request)
                 (xmid/pre-route-middlewares)
                 (route)
                 (xmid/pre-controller-middlewares)
                 (run-controller)
                 (xmid/post-controller-middlewares))
               (xiana/extract)
               (get :response)))))
  (stop [this]
        (assoc this :handler nil)))

(defn make-app
  [config]
  (map->App {:config config}))
