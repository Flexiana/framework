(ns framework.components.app.components
  (:require
    [clj-http.client :as http]
    [clojure.test :refer :all]
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

(deftest via-http-request
  (is (= {:status 200 :body "Ok"}
         (-> (http/request {:method :get :url "http://localhost:3000/interceptor"})
             (select-keys [:status :body])))))
