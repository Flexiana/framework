(ns xiana.interceptor.cors-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [xiana.interceptor.cors :refer [cors-headers interceptor]]))

(deftest cors-interceptor-test
  (testing "cross-origin-headers interceptor"
    (let [leave (:leave interceptor)
          origin "http://localhost:3001"]

      (testing "when request is a preflight request"
        (let [state {:request {:request-method :options}
                     :deps    {:cors-origin origin}}
              expected (update-in state [:response]
                                  merge
                                  {:status  200
                                   :headers (cors-headers origin)
                                   :body    "preflight complete"})
              result (leave state)]
          (is (= expected result))))

      (testing "when request is not a preflight request"
        (let [state {:request  {:request-method :get}
                     :deps     {:cors-origin origin}
                     :response {:headers {}}}
              expected (update-in state [:response :headers]
                                  merge (cors-headers origin))
              result (leave state)]
          (is (= expected result)))))))
