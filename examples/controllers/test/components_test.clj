(ns components-test
  (:require
    [clj-http.client :as http]
    [clojure.test :refer [deftest is use-fixtures]]
    [com.stuartsierra.component :as component]
    [components :as comps]
    [framework.config.core :as config]))

(defn std-system-fixture
  [f]
  (let [config (config/edn)
        system (-> config
                   comps/system
                   component/start)]
    (try
      (f)
      (finally
        (component/stop system)))))

(use-fixtures :each std-system-fixture)

(deftest testing-controllers
  (is (= {:body   "Unauthorized"
          :status 401}
         (-> {:url                  "http://localhost:3000/"
              :unexceptional-status (constantly true)
              :method               :get}
             http/request
             (select-keys [:status :body]))))
  (is (= {:body   "Unauthorized"
          :status 401}
         (-> {:url                  "http://localhost:3000/"
              :unexceptional-status (constantly true)
              :method               :get}
             http/request
             (select-keys [:status :body]))))
  (is (= {:body   "Index page"
          :status 200}
         (-> {:url                  "http://localhost:3000/"
              :unexceptional-status (constantly true)
              :basic-auth           ["aladdin" "opensesame"]
              :method               :get}
             http/request
             (select-keys [:status :body]))))
  (is (= {:body   "Not Found"
          :status 404}
         (-> {:url                  "http://localhost:3000/wrong"
              :unexceptional-status (constantly true)
              :basic-auth           ["aladdin" "opensesame"]
              :method               :get}
             http/request
             (select-keys [:status :body])))))
