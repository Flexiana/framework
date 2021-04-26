(ns framework.components.web-server.core-test
  (:require
    [clojure.test :refer [deftest is use-fixtures]]
    [framework-fixture :refer [std-system-fixture system app-config routes]]
    [framework.components.web-server.core :as web-server]
    [framework.config.core :as config]))

(use-fixtures :once std-system-fixture)

(deftest components
  (let [req     {:request-method :get :uri "/users"}
        sys     (system (config/edn) app-config routes)
        handler (web-server/handler-fn app-config sys routes)
        state   (web-server/state-build app-config sys routes req)]
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
