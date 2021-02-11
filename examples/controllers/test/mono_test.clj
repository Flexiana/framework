(ns mono-test
  (:require
    [clojure.test :refer :all]
    [kerodon.core :refer :all]
    [kerodon.test :refer :all]
    [mono]))

(deftest test-mono-router
  (-> (session mono/app)
      (visit "/pong")
      (has (status? 200))))
