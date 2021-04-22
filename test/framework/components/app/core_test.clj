(ns framework.components.app.core-test
  (:require
    [clj-http.client :as http]
    [clojure.data.json :as json]
    [clojure.test :refer [deftest is use-fixtures]]
    [framework-fixture :refer [std-system-fixture st app-config routes]]
    [framework.components.session.backend :refer [fetch add!]]
    [framework.components.web-server.core :as web-server]))

(use-fixtures :once std-system-fixture)

(deftest components
  (let [req {:request-method :get :uri "/users"}
        state (web-server/state-build app-config @st routes req)
        session-backend (-> state :deps :session-backend)
        test-value "test-value"]
    (is (= req (:request state)))
    (is (instance? Object session-backend))
    (is (= test-value
           (do (add! session-backend :test test-value)
               (fetch session-backend :test))))
    (is (= {:hash-algorithm  :bcrypt,
            :bcrypt-settings {:work-factor 11},
            :scrypt-settings {:cpu-cost        32768,
                              :memory-cost     8,
                              :parallelization 1},
            :pbkdf2-settings {:type       :sha1,
                              :iterations 100000}}
           (get-in state [:deps :auth])))
    (is (= {"users" [:read :create :update :delete]}
           (:acl/permissions state)))
    (is (= {:admin [{:resource :all,
                     :actions  [:all],
                     :over     :all}]}
           (:acl/roles state)))))

(deftest add-interceptor
  (let [req {:request-method :get :uri "/interceptor"}
        handler (web-server/handler-fn app-config @st routes)
        response (handler req)]
    (is (= {:enter  true
            :status 200
            :body   "Ok"
            :leave  true}
           (dissoc response :headers)))
    (is (string?
          (get-in response [:headers "Session-id"])))))

(deftest via-http-request
  (is (= {:status 200 :body "Ok"}
         (-> (http/request {:method :get :url "http://localhost:3000/interceptor"})
             (select-keys [:status :body])))))

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
         (-> {:url                  "http://localhost:3000/session"
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
         (-> {:url                  "http://localhost:3000/session"
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
