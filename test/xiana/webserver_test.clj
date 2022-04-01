(ns xiana.webserver-test
  (:require
    [clojure.test :refer :all]
    [xiana.core :as xiana]
    [xiana.handler :refer [handler-fn]]
    [xiana.route :as route]))

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
