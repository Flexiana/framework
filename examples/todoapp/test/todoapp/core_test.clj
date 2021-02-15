(ns todoapp.main-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [framework.config.core :as config]
            [clj-http.client :as http]
            [todoapp.core :as todoapp]
            [com.stuartsierra.component :as component]
            [jsonista.core :as json]
            [clojure.string :as str]))

(defn json-read [v]
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
