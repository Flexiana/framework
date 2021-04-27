(ns framework.components.web-server.core
  (:require
    [com.stuartsierra.component :as component]
    [framework.components.runner :as runner]
    [reitit.core :as r]
    [reitit.http :as http]
    [reitit.interceptor.sieppari :as sieppari]
    [reitit.ring :as ring]
    [reitit.swagger-ui :as swagger-ui]
    [ring.adapter.jetty :as jetty]
    [xiana.commons :refer [?assoc-in]]
    [xiana.core :as xiana])
  (:import
    (org.eclipse.jetty.server
      Server)))

(defn default-action
  [{request :request {handler :handler} :request-data :as state}]
  (try
    (xiana/ok (assoc state :response (handler request)))
    (catch Exception e
      (xiana/error (-> state
                       (assoc :controller-error e)
                       (assoc :response {:status 500 :body "Internal Server error"}))))))

(defn response
  [state response]
  (assoc state :response response))

(defn route
  [{request          :request
    {router :router} :deps
    :as              state}]
  (let [match   (r/match-by-path router (:uri request))
        method  (:request-method request)
        handler (or (get-in match [:data :handler]) (-> match :result method :handler))
        action  (or (get-in match [:data :action]) (-> match :data method :action))]
    (if action
      (xiana/ok (-> state
                    (?assoc-in [:request-data :match] match)
                    (?assoc-in [:request-data :handler] handler)
                    (assoc-in [:request-data :action] action)))

      (if handler
        (xiana/ok (-> state
                      (?assoc-in [:request-data :match] match)
                      (assoc-in [:request-data :handler] handler)
                      (assoc-in [:request-data :action] default-action)))
        (xiana/error (response state {:status 404 :body "Not Found"}))))))

(defn run-controller
  [state]
  (let [controller (get-in state [:request-data :action])]
    (controller state)))

(defn state-build
  [{:keys [acl-cfg
           session-backend
           auth]}
   {:keys [db]}
   routes
   http-request]
  (-> {:deps    {:router          (ring/router routes)
                 :db              db
                 :session-backend session-backend
                 :auth            auth}
       :request http-request}
      xiana/map->State
      (conj acl-cfg)))

(defn handler-fn
  [{:keys [controller-interceptors
           router-interceptors]
    :as   app-config}
   system
   routes]
  (fn [http-request]
    (->
      (xiana/flow->
        (state-build app-config system routes http-request)
        (runner/run router-interceptors route)
        (runner/run controller-interceptors run-controller))
      (xiana/extract)
      (get :response))))

(defn ->web-server
  [{web-cfg :framework.app/web-server
    :as     config}
   app-config
   routes]
  (with-meta config
    `{component/start ~(fn [system]
                         (assoc system :web-server
                           (jetty/run-jetty (handler-fn app-config system routes) web-cfg)))
      component/stop  ~(fn [{:keys [^Server web-server]
                             :as   system}]
                         (.stop web-server)
                         (dissoc system :web-server))}))

(defn interceptor-router
  [config app-cfg routes]
  (http/router
    (routes config)
    app-cfg))

(def ring-routes
  (ring/routes
    (swagger-ui/create-swagger-ui-handler
      {:path   "/api-docs"
       :config {:validatorUrl     nil
                :operationsSorter "alpha"}})
    (ring/create-default-handler)))

(defn reitit-handler
  [config app-cfg routes]
  (http/ring-handler
    (interceptor-router config app-cfg routes)
    ring-routes
    {:executor sieppari/executor}))

(defn ->reitit-web-server
  [{web-cfg :framework.app/web-server
    :as     config}
   app-cfg
   routes]
  (with-meta config
    `{component/start ~(fn [system]
                         (assoc system :web-server
                           (jetty/run-jetty (reitit-handler system app-cfg routes) web-cfg)))
      component/stop  ~(fn [{:keys [^Server web-server]
                             :as   system}]
                         (.stop web-server)
                         (dissoc system :web-server))}))
