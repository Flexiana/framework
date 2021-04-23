(ns framework.components.app.one-endpoint-test
  (:require
    [clj-http.client :as http]
    [clojure.data.json :as json]
    [clojure.test :refer [deftest is use-fixtures]]
    [framework-fixture :refer [std-system-fixture]]))

(use-fixtures :once std-system-fixture)

(deftest same-response
  (is (= #{{:users/last_login nil,
            :users/username   "admin",
            :users/created_at "2021-03-30",
            :users/role       "admin",
            :users/email      "admin@frankie.sw",
            :users/id         "fd5e0d70-506a-45cc-84d5-b12b5e3e99d2",
            :users/password   "$2a$11$ivfRMKD7dHMfqCWBiEQcaOknsJgDnK9zoSP/cXAVNQVYHc.M9SZJK",
            :users/is_active  true,
            :users/fullname   nil,
            :users/salt       nil}
           {:users/created_at "2021-03-30",
            :users/email      "frankie@frankie.sw",
            :users/fullname   nil,
            :users/id         "31c2c58f-28cb-4013-8765-9240626a18a2",
            :users/is_active  true,
            :users/last_login nil,
            :users/password   "$2a$11$ivfRMKD7dHMfqCWBiEQcaOknsJgDnK9zoSP/cXAVNQVYHc.M9SZJK",
            :users/role       "user",
            :users/salt       nil,
            :users/username   "frankie"}
           {:users/created_at "2021-03-30",
            :users/email      "impostor@frankie.sw",
            :users/fullname   nil,
            :users/id         "8d05b2e1-6463-478a-ba30-35768738af29",
            :users/is_active  false,
            :users/last_login nil,
            :users/password   "$2a$11$ivfRMKD7dHMfqCWBiEQcaOknsJgDnK9zoSP/cXAVNQVYHc.M9SZJK",
            :users/role       "interviewer",
            :users/salt       nil,
            :users/username   "impostor"}}
         (-> {:url                  "http://localhost:3000/action"
              :unexceptional-status (constantly true)
              :method               :post
              :headers              {"Content-Type" "application/json;charset=utf-8"}
              :body                 (json/write-str {:action   "get"
                                                     :resource "users"})}
             http/request
             :body
             (json/read-str :key-fn keyword)
             :data
             :users
             set)
         (-> {:method               :get
              :unexceptional-status (constantly true)
              :headers              {"Content-Type" "application/json;charset=utf-8"}
              :url                  "http://localhost:3000/users"}
             http/request
             :body
             (json/read-str :key-fn keyword)
             :data
             :users
             set))))

(deftest get-by-id
  (is (= [{:users/created_at "2021-03-30",
           :users/email      "frankie@frankie.sw",
           :users/fullname   nil,
           :users/id         "31c2c58f-28cb-4013-8765-9240626a18a2",
           :users/is_active  true,
           :users/last_login nil,
           :users/password   "$2a$11$ivfRMKD7dHMfqCWBiEQcaOknsJgDnK9zoSP/cXAVNQVYHc.M9SZJK",
           :users/role       "user",
           :users/salt       nil,
           :users/username   "frankie"}]
         (-> {:method               :get
              :unexceptional-status (constantly true)
              :query-params         {:id "31c2c58f-28cb-4013-8765-9240626a18a2"}
              :headers              {"Content-Type" "application/json;charset=utf-8"}
              :url                  "http://localhost:3000/users"}
             http/request
             :body
             (json/read-str :key-fn keyword)
             :data
             :users)
         (-> {:url                  "http://localhost:3000/action"
              :unexceptional-status (constantly true)
              :method               :post
              :headers              {"Content-Type" "application/json;charset=utf-8"}
              :body                 (json/write-str {:action   "get"
                                                     :id       "31c2c58f-28cb-4013-8765-9240626a18a2",
                                                     :resource "users"})}
             http/request
             :body
             (json/read-str :key-fn keyword)
             :data
             :users))))

(-> {:url                  "http://localhost:3000/session"
     :unexceptional-status (constantly true)
     :method               :post
     :headers              {"Content-Type" "application/json;charset=utf-8"}
     :body                 (json/write-str {:action   "get"
                                            :id       "31c2c58f-28cb-4013-8765-9240626a18a2",
                                            :resource "users"})}
    http/request
    :body
    (json/read-str :key-fn keyword))
;    (#(map-commit {} %)))
