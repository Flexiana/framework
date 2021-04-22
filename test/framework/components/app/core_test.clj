(ns framework.components.app.core-test
  (:require
    [clojure.test :refer [deftest is use-fixtures]]
    [framework-fixture :refer [std-system-fixture st app-config routes]]
    [framework.components.web-server.core :as web-server]))

(use-fixtures :once std-system-fixture)

(deftest components
  (let [req     {:request-method :get :uri "/users"}
        handler (web-server/handler-fn app-config @st routes)
        state   (web-server/state-build app-config @st routes req)]
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
    (is (= {:enter true
            :status 200
            :body "Ok",
            :leave true}
           (dissoc response :headers)))
    (is (string?
          (get-in response [:headers "Session-id"])))))