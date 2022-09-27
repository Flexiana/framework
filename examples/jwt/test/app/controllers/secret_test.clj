(ns app.controllers.secret-test
  (:require
    [app.controllers.login :refer [login-controller]]
    [app.controllers.secret :refer [protected-controller]]
    [clojure.test :refer :all]
    [xiana.jwt :as jwt]
    [xiana.jwt.interceptors :as jwt-interceptors]))

(defn auth-token
  [config]
  (let [state {:request {:body-params {:email    "xiana@test.com"
                                       :password "topsecret"}}
               :deps    config}]
    (-> (login-controller state)
        :response
        :body
        :auth-token)))

(deftest protected-controller-test
  (let [interceptor (:enter jwt-interceptors/jwt-auth)
        private-key-file "resources/_files/jwtRS256.key"
        public-key-file "resources/_files/jwtRS256.key.pub"
        config (jwt/init-from-file
                 {:xiana/jwt {:auth {:alg         :rs256
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
        auth (auth-token config)
        request {:headers     {:authorization (str "Bearer " auth)}
                 :body-params {:hello "hello"}}]
    (is (= "Hello Xiana. request content: {:hello \"hello\"}"
           (-> {:request request
                :deps    config}
               interceptor
               protected-controller
               :response
               :body)))))
