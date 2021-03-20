(ns status-test
  (:require
    [app]
    [clojure.test :refer :all]
    [framework.config.core :as config]
    [kerodon.core :refer :all]
    [kerodon.test :refer :all]))

(deftest status-test
  (let [config (config/edn)
        app-cfg (:framework.app/ring config)
        handler (controllers.posts/controller app-cfg)]
    (-> (session handler)
        (visit "/status")
        (has (status? 200))
        (has (some-regex? "^.*OK.*$")))))
