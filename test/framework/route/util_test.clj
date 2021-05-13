(ns framework.route.util-test
  (:require
   [xiana.core :as xiana]
   [clojure.test :refer :all]
   [framework.route.util :as util]))

(defn test-handler
  "Sample test handler function for the tests."
  [_]
  {:status 200 :body "Ok"})

(def test-state
  "Sample test state."
  {:request {}
   :request-data {:handler test-handler}})

;; test default not-found handler response
(deftest not-found-response
  (let [response (:response (xiana/extract (util/not-found {})))
        expected {:status 404, :body "Not Found"}]
    ;; verify if the response and expected value are equal
    (is (= response expected))))

;; test default action handler: error response
(deftest action-error-response
  (let [response (:response (xiana/extract (util/action {})))
        expected {:status 500 :body "Internal Server error"}]
    ;; verify if the response and expected value are equal
    (is (= response expected))))

;; test default action handler: ok response
(deftest action-ok-response
  (let [response (:response (xiana/extract (util/action test-state)))
        expected {:status 200, :body "Ok"}]
    ;; verify if the response and expected value are equal
    (is (= response expected))))
