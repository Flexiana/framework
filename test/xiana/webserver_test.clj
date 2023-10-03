(ns xiana.webserver-test
  (:require
    [clojure.test :refer [deftest function? is]]
    [xiana.handler :refer [handler-fn]]
    [xiana.route :as route]))

(def default-interceptors [])

(def sample-request
  {:uri "/" :request-method :get})

(def sample-routes
  "Sample routes structure."
  {:routes [["/" {:action #(assoc % :response {:status 200, :body ":action"})}]]})

(deftest handler-fn-creation
  ;; test if handler-fn return
  (let [handler-fn (handler-fn {:controller-interceptors default-interceptors})]
    ;; check if return handler is a function
    (is (function? handler-fn))))

;; test jetty handler function call
(deftest call-handler-fn
  (let [f (handler-fn (route/reset sample-routes))]
    ;; verify if it's the right response
    (is (= (f sample-request) {:status 200, :body ":action"}))))
