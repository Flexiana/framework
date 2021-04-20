(ns framework.components.app.core-test
  (:require
    [clojure.test :refer :all]
    [framework-fixture :refer [std-system-fixture st]]))

(use-fixtures :once std-system-fixture)

(deftest components
  (let [handler (get-in @st [:app :handler])]
    (is (= {:status 200, :body "Ok"}
           (:response (handler {:request-method :get :uri "/users"}))))
    (is (not (nil? (get-in @st [:app :router :router]))))
    (is (not (nil? (get-in @st [:app :db]))))
    (is (not (nil? (get-in @st [:app :auth]))))
    (is (not (nil? (get-in @st [:app :session-backend]))))
    (is (= 2 (count (get-in @st [:app :acl-cfg]))))))
