(ns xiana.jwt-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [xiana.jwt :as sut]))

(def test-private-key (slurp "test/resources/_files/jwtRS256.key"))

(def test-public-key (slurp "test/resources/_files/jwtRS256.key.pub"))

(def config
  {:xiana/jwt
   {:auth {:public-key test-public-key
           :private-key test-private-key
           :alg :rs256
           :in-claims {:iss "fake-issuer"
                       :aud "fake-audience"
                       :sub "fake-subject"
                       :leeway 0
                       :max-age 40}
           :out-claims {:exp 10
                        :iss "fake-issuer"
                        :aud "fake-audience"
                        :sub "fake-subject"
                        :nbf 0}}
    :content {:public-key test-public-key
              :private-key test-private-key
              :alg :rs256}}})

(defn sign-and-verify-jwt
  [type payload cfg]
  (try
    (sut/verify-jwt type
                    (sut/sign type payload cfg)
                    cfg)
    (catch clojure.lang.ExceptionInfo e
      (ex-data e))))

(deftest test-jwt-auth
  (let [cfg (get-in config [:xiana/jwt :auth])
        type    :auth
        payload {:user "test"}]

    (testing "Sign and verify succeeded"
      (let [claims (:out-claims cfg)
            out-claims (#'sut/calculate-time-claims claims)]
        (is (= (merge payload out-claims)
               (sign-and-verify-jwt type payload cfg)))))

    (testing "Sign and verify expired token"
      (let [cfg (assoc-in cfg [:out-claims :exp] -1)]
        (is (= {:type :validation :cause :exp}
               (sign-and-verify-jwt type payload cfg)))))

    (testing "Sign and verify token with invalid nbf (not before) claim."
      (let [cfg (assoc-in cfg [:out-claims :nbf] 10)]
        (is (= {:type :validation :cause :nbf}
               (sign-and-verify-jwt type payload cfg)))))

    (testing "Token is older than max-age"
      (let [cfg (assoc-in cfg [:in-claims :max-age] -1)]
        (is (= {:type :validation :cause :max-age}
               (sign-and-verify-jwt type payload cfg)))))))


(deftest test-jwt-content
  (let [cfg (get-in config [:xiana/jwt :content])
        type :content
        payload {:test "a" :foo "bar"}]
    (testing "JWT content exchange succeeded"
      (is (= payload
             (sign-and-verify-jwt type payload cfg))))))
