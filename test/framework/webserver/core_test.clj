(ns framework.webserver.core-test
  (:require
    [clojure.test :refer :all]
    [framework.handler.core :refer [handler-fn]]
    [framework.route.core :as route]
    [framework.webserver.core :as webserver]
    [framework.webserver.core :as ws]
    [org.httpkit.server :refer [server-status]]
    [xiana.core :as xiana]))

(def default-interceptors [])

(def sample-request
  {:uri "/" :request-method :get})

(def sample-routes
  "Sample routes structure."
  {:routes [["/" {:action
                  #(xiana/ok
                     (assoc % :response {:status 200, :body ":action"}))}]]})

(deftest handler-fn-creation
  ;; test if handler-fn return
  (let [handler-fn (handler-fn {:controller-interceptors default-interceptors})]
    ;; check if return handler is a function
    (is (function? handler-fn))))

;; test jetty handler function call
(deftest call-handler-fn
  ;; set sample routes

  ;; set handler function
  (let [f (handler-fn (route/reset sample-routes))]
    ;; verify if it's the right response
    (is (= (f sample-request) {:status 200, :body ":action"}))))

(deftest stop-webserver
  (ws/stop)
  (is (or (empty? @webserver/-webserver)
          (= :stopped (-> @webserver/-webserver
                          :server
                          meta
                          :server
                          server-status)))))

