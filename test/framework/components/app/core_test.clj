(ns framework.components.app.core-test
  (:require
    [clojure.test :refer :all]
    [framework-fixture :refer [std-system-fixture st app-config]]
    [framework.components.app.core :as app]))

(use-fixtures :once std-system-fixture)

(deftest components
  (let [req {:request-method :get :uri "/users"}
        handler (get-in @st [:app :handler])
        state (app/state-build @app-config @st req)]
    (is (= {:status 200, :body "Ok"}
           (:response (handler req))))
    (is (= req (:request state)))
    (is (= :reitit.core/router
           (type (get-in state [:deps :router]))))
    (is (= framework.components.session.backend$init_in_memory_session$reify__5151
           (type (get-in state [:deps :session-backend]))))
    (is (= {:hash-algorithm  :bcrypt,
            :bcrypt-settings {:work-factor 11},
            :scrypt-settings {:cpu-cost        32768,
                              :memory-cost     8,
                              :parallelization 1},
            :pbkdf2-settings {:type       :sha1,
                              :iterations 100000}}
           (get-in state [:deps :auth])))
    (is (= #:acl{:permissions {"users" [:read :create :update :delete]},
                 :roles       {:admin [{:resource :all,
                                        :actions  [:all],
                                        :over     :all}]}}
           (:acl-cfg state)))
    (is (not (nil? (get-in @st [:app :router :router]))))
    (is (not (nil? (get-in @st [:app :db]))))
    (is (not (nil? (get-in @st [:app :auth]))))
    (is (not (nil? (get-in @st [:app :session-backend]))))
    (is (= 2 (count (get-in @st [:app :acl-cfg]))))))
