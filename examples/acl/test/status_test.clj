(ns status-test
  (:require [app]
            [framework.config.core :as config]
            [kerodon.core :refer :all]
            [kerodon.test :refer :all]
            [clojure.test :refer :all]))

(deftest status-test
  (let [config (config/edn)
        app-cfg (:framework.app/ring config)
        handler (app/ring-app app-cfg)]
      (-> (session handler)
          (visit "/status")
          (has (status? 200))
          (has (some-regex? "^.*OK.*$")))))
