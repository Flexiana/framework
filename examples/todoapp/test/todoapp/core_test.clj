(ns todoapp.main-test
  (:require
    [clj-http.client :as http]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [com.stuartsierra.component :as component]
    [framework.config.core :as config]
    [jsonista.core :as json]
    [todoapp.core :as todoapp]))

(defn json-read
  [v]
  (if-not (str/blank? v)
    (json/read-value v)
    v))

(defn std-system-fixture
  [f]
  (let [config (config/edn)
        system (-> config
                   todoapp/system
                   component/start)]
    (try
      (f)
      (finally
        (component/stop system)))))

(use-fixtures :each std-system-fixture)

(deftest testing-controllers
  (is (= {:body   [{"todo/id" 1 "todo/value" "ok"}]
          :status 202}
         (-> {:url                  "http://localhost:3000/hello"
              :unexceptional-status (constantly true)
              :method               :get}
             http/request
             (select-keys [:status :body])
             (update :body json-read)))))
