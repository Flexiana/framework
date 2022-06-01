(ns xiana.state-test
  (:require
    [clojure.test :refer :all]
    [xiana.state :as state]))

(def state-initial-map
  {:deps     {}
   :request  {}
   :response {}})

;; test empty state creation
(deftest initial-state
  (let [result (state/make {} {})
        expected (state/map->State state-initial-map)]
    ;; verify if the response and expected value are equal
    (is (= result expected))))
