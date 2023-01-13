(ns app.controllers.login-test
  (:require
    [app.controllers.login :refer [login-controller]]
    [clojure.test :refer :all]
    [xiana.jwt :as jwt])
  (:import
    (java.util
      Base64)))

(deftest login-controller-test
  (testing "use string keys"
    (let [private-key-file "resources/_files/jwtRS256.key"
          public-key-file "resources/_files/jwtRS256.key.pub"
          test-private-key (slurp private-key-file)
          test-public-key (slurp public-key-file)
          config {:xiana/jwt {:auth {:alg         :rs256
                                     :public-key  test-public-key
                                     :private-key test-private-key
                                     :in-claims   {:iss     "xiana-api"
                                                   :aud     "api-consumer"
                                                   :leeway  0
                                                   :max-age 40}
                                     :out-claims  {:exp 1000
                                                   :iss "xiana-api"
                                                   :aud "api-consumer"
                                                   :nbf 0}}}}
          state {:request {:body-params {:email    "xiana@test.com"
                                         :password "topsecret"}}
                 :deps    config}]
      (is (some? (-> (login-controller state)
                     :response
                     :body
                     :auth-token)))))
  (testing "use file keys"
    (let [private-key-file "resources/_files/jwtRS256.key"
          public-key-file "resources/_files/jwtRS256.key.pub"
          config (jwt/init-from-file {:xiana/jwt {:auth {:alg         :rs256
                                                         :public-key  public-key-file
                                                         :private-key private-key-file
                                                         :in-claims   {:iss     "xiana-api"
                                                                       :aud     "api-consumer"
                                                                       :leeway  0
                                                                       :max-age 40}
                                                         :out-claims  {:exp 1000
                                                                       :iss "xiana-api"
                                                                       :aud "api-consumer"
                                                                       :nbf 0}}}})
          state {:request {:body-params {:email    "xiana@test.com"
                                         :password "topsecret"}}
                 :deps    config}]
      (is (some? (-> (login-controller state)
                     :response
                     :body
                     :auth-token)))))
  (testing "use B64 encoded"
    (let [private-key-file "resources/_files/jwtRS256.key"
          public-key-file "resources/_files/jwtRS256.key.pub"
          private-key (.encodeToString (Base64/getEncoder) (.getBytes (slurp private-key-file)))
          public-key (.encodeToString (Base64/getEncoder) (.getBytes (slurp public-key-file)))
          config (jwt/init-from-base64 {:xiana/jwt {:auth {:alg         :rs256
                                                           :public-key  public-key
                                                           :private-key private-key
                                                           :in-claims   {:iss     "xiana-api"
                                                                         :aud     "api-consumer"
                                                                         :leeway  0
                                                                         :max-age 40}
                                                           :out-claims  {:exp 1000
                                                                         :iss "xiana-api"
                                                                         :aud "api-consumer"
                                                                         :nbf 0}}}})
          state {:request {:body-params {:email    "xiana@test.com"
                                         :password "topsecret"}}
                 :deps    config}]
      (is (some? (-> (login-controller state)
                     :response
                     :body
                     :auth-token))))))

