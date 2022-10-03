(ns integration-test
  (:require
    [clj-http.client :refer [request]]
    [clojure.data.json :as json]
    [clojure.test :refer [deftest is use-fixtures]]
    [jwt-fixture :refer [std-system-fixture]]))

(use-fixtures :once std-system-fixture)

(defn bearer
  [auth-key]
  {"Authorization" (str "Bearer " auth-key)})

(def email "xiana@test.com")
(def password "topsecret")

(defn auth
  [email password]
  (-> (request {:method  :post
                :url     "http://localhost:3333/login"
                :headers {"Content-Type" "application/json;charset=utf-8"}
                :body    (json/write-str {:email    email
                                          :password password})})
      :body
      (json/read-str :key-fn keyword)
      :auth-token))

(deftest unauthorized-secret
  (let [response (request {:method               :post
                           :unexceptional-status (constantly true)
                           :url                  "http://localhost:3333/secret"
                           :headers              {"Content-Type" "application/json;charset=utf-8"}
                           :body                 (json/write-str {:hello "hello"})})]
    (is (= 401 (:status response)))
    (is (= "Signature could not be verified." (:body response)))))

(deftest login-test
  (is (some? (auth email password))))

(deftest authorized-secret
  (let [auth-token (auth email password)
        response   (request {:method               :post
                             :unexceptional-status (constantly true)
                             :url                  "http://localhost:3333/secret"
                             :headers              (merge {"Content-Type" "application/json;charset=utf-8"}
                                                          (bearer auth-token))
                             :body                 (json/write-str {:hello "hello"})})]
    (is (= 200 (:status response)))
    (is (= "Hello Xiana. request content: {:hello \"hello\"}" (:body response)))))
