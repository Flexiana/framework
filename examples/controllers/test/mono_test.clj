(ns mono-test
  (:require [mono]
            [kerodon.core :refer :all]
            [kerodon.test :refer :all]
            [clojure.test :refer :all]))

(deftest test-mono-router
  (-> (session mono/app)
      (visit "/pong")
      (has (status? 200))))
