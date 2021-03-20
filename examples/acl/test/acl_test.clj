(ns acl-test
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

(use-fixtures :once std-system-fixture)

