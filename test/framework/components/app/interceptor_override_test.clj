(ns framework.components.app.interceptor-override-test
  (:require
    [framework-fixture :refer [std-system-fixture]]
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [clj-http.client :as http]))

(use-fixtures :once std-system-fixture)
(deftest interceptor-override
  (let [response (-> {:url                  "http://localhost:3000/test-override"
                      :unexceptional-status (constantly true)
                      :method               :post
                      :headers              {"Content-Type" "application/json;charset=utf-8"}
                      :body                 (json/write-str {:action   "get"
                                                             :id       "31c2c58f-28cb-4013-8765-9240626a18a2",
                                                             :resource "users"})}
                     http/request)]
    (is (= {:status 200 :body "Ok"}
           (select-keys response [:status :body])))
    (is (= "true"
           (get-in response [:headers "override-in"])))
    (is (= "true"
           (get-in response [:headers "override-out"])))
    (is (nil?
          (get-in response [:headers "Session-id"])))))