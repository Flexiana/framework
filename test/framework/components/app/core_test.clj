(ns framework.components.app.core-test
  (:require
    [clj-http.client :as http]
    [clojure.data.json :as json]
    [clojure.test :refer [deftest is use-fixtures]]
    [framework-fixture :refer [std-system-fixture st app-config routes]]
    [framework.components.web-server.core :as web-server]))

(use-fixtures :once std-system-fixture)

(deftest components
  (let [req {:request-method :get :uri "/users"}
        handler (web-server/handler-fn app-config @st routes)
        state (web-server/state-build app-config @st routes req)]
    (def _mst state)
    (is (= {:status 200, :body "Ok"}
           (:response (handler req))))
    (is (= req (:request state)))
    (is (instance? Object (get-in state [:deps :session-backend])))
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

(deftest one-endpoint-routing
  (is (= #:users{:last_login nil,
                 :username   "admin",
                 :created_at "2021-03-30",
                 :role       "admin",
                 :email      "admin@frankie.sw",
                 :id         "fd5e0d70-506a-45cc-84d5-b12b5e3e99d2",
                 :password   "$2a$11$ivfRMKD7dHMfqCWBiEQcaOknsJgDnK9zoSP/cXAVNQVYHc.M9SZJK",
                 :is_active  true,
                 :fullname   nil,
                 :salt       nil}
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
             :users))))
