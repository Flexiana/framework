(ns framework.components.app.interceptor-around-test
  (:require
    [clojure.test :refer :all]
    [framework-fixture :refer [std-system-fixture st app-config routes]]
    [framework.components.web-server.core :as web-server]))

(use-fixtures :once std-system-fixture)

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
