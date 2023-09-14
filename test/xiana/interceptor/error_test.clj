(ns xiana.interceptor.error-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [xiana.interceptor.error :refer [response]]))

(defn make-error-state [response]
  {:error (ex-info "Test exception" {:xiana/response response})})

(deftest response-test
  (testing "Error response interceptor"
    (let [resp {:error "Test error"}
          error-state (make-error-state resp)
          f (:error response)
          result (f error-state)]
      (is (= {:response resp} result))

      (testing "When :error is nil"
        (let [error-state {:error nil}
              result (f error-state)]
          (is (= {:error nil} result)))))))
