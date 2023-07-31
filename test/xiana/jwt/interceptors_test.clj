(ns xiana.jwt.interceptors-test
  (:require [clojure.test :refer [deftest is testing]]
            [xiana.jwt :as jwt]
            [xiana.jwt.interceptors :refer [jwt-auth]]
            [xiana.route.helpers :as helpers]))

(defn mock-jwt-verify [_ _ _] "mocky")

(defn mock-unauthorized [state _]
  (assoc state :unauthorized true))

(deftest jwt-auth-test
  (with-redefs [jwt/verify-jwt mock-jwt-verify
                helpers/unauthorized mock-unauthorized]

    (testing "jwt-authentication interceptor"
      (let [enter-fn (:enter jwt-auth)
            error-fn (:error jwt-auth)
            auth-token "your-auth-token"
            jwt-cfg {:auth "auth-config"}]

        (testing "enter function - with :options request method"
          (let [state {:request {:request-method :options}}
                result (enter-fn state)]
            (is (= state result))))

        (testing "enter function - with authorization in headers"
          (let [state {:request {:request-method :get
                                 :headers        {:authorization (str "Bearer " auth-token)}}}
                expected (assoc-in state [:session-data :jwt-authentication] (jwt/verify-jwt :claims auth-token jwt-cfg))
                result (enter-fn state)]
            (is (= expected result))))

        (testing "enter function - without authorization in headers"
          (let [state {:request {:request-method :get
                                 :headers        {:authorization nil}}}
                expected "Authorization header not provided"
                result (enter-fn state)]
            (is (= expected (.getMessage (:error result))))))

        (testing "error function - with :exp cause"
          (let [state {:error (ex-info "Error message" {:cause :exp})}
                expected (helpers/unauthorized state "JWT Token expired.")
                result (error-fn state)]
            (is (= expected result))))

        (testing "error function - with :validation type"
          (let [state {:error (ex-info "Error message" {:type :validation})}
                expected (helpers/unauthorized state "One or more Claims were invalid.")
                result (error-fn state)]
            (is (= expected result))))

        (testing "error function - with other error type"
          (let [state {:error (ex-info "Error message" {:type :other})}
                expected (helpers/unauthorized state "Signature could not be verified.")
                result (error-fn state)]
            (is (= expected result))))))))
