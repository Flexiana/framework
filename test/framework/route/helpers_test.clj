(ns xiana.route.helpers-test
  (:require
    [clojure.test :refer :all]
    [xiana.core :as xiana]
    [xiana.route.helpers :as helpers]))

(defn test-handler
  "Sample test handler function for the tests."
  [_]
  {:status 200 :body "Ok"})

(def test-state
  "Sample test state."
  {:request {}
   :request-data {:handler test-handler}})

;; test default not-found handler response
(deftest contains-not-found-response
  (let [response (:response (xiana/extract
                              (helpers/not-found {})))
        expected {:status 404, :body "Not Found"}]
    ;; verify if the response and expected value are equal
    (is (= response expected))))

;; test default action handler: error response
(deftest contains-action-error-response
  (let [response (:response (xiana/extract
                              (helpers/action {})))
        expected {:status 500 :body "Internal Server error"}]
    ;; verify if the response and expected value are equal
    (is (= response expected))))

;; test default action handler: ok response
(deftest contains-action-ok-response
  (let [response (:response (xiana/extract
                              (helpers/action test-state)))
        expected {:status 200, :body "Ok"}]
    ;; verify if the response and expected value are equal
    (is (= response expected))))
