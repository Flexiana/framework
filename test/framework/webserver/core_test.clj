(ns framework.webserver.core-test
  (:require
    [clojure.test :refer :all]
    [framework.route.core :as route]
    [framework.state.core :as state]
    [framework.webserver.core :as webserver]
    [xiana.core :as xiana]))

(def default-interceptors [])

(def sample-request
  {:uri "/" :request-method :get})

(def sample-routes
  "Sample routes structure."
  [["/" {:action
         #(xiana/ok
            (assoc % :response {:status 200, :body ":action"}))}]])

(deftest handler-fn-creation
  ;; test if handler-fn return
  (let [handler-fn (webserver/handler-fn {:controller-interceptors default-interceptors})]
    ;; check if return handler is a function
    (is (function? handler-fn))))

(deftest start-webserver
  ;; verify if initial instance is clean
  (is (empty? @webserver/-webserver))
  ;; start the server and fetch it
  (let [result (webserver/start {:controller-interceptors default-interceptors})
        server (:server result)]
    ;; verify if server object was properly set
    (is (= (type server)
           org.eclipse.jetty.server.Server))))

;; test jetty handler function call
(deftest call-jetty-handler-fn
  ;; set sample routes
  (route/reset sample-routes)
  ;; set handler function
  (let [f (webserver/handler-fn default-interceptors)]
    ;; verify if it's the right response
    (is (= (f sample-request) {:status 200, :body ":action"}))))

;; TODO: research: this is wrong, because web-server stop function
;; always returns nil
(deftest stop-webserver
  ;; how to verify if the following function call was successful?
  (is (nil? (webserver/stop))))

