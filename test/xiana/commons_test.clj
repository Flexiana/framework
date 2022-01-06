(ns xiana.commons-test
  (:require
   [clojure.test :refer [deftest are]]
   [xiana.commons :refer [deep-merge]]))

(deftest deep-merge-test
  (are [res a b] (= res (deep-merge a b))
    nil              nil         nil
    {:a 1}           nil         {:a 1}
    {:a 1}           {:a 1}      nil
    {:a 1 :b 2}      {:a 1}      {:b 2}
    {:a 2}           {:a 1}      {:a 2}
    {:a [2]}         {:a [1]}    {:a [2]}
    {:a nil}         {:a 1}      {:a nil}
    {:a {:m 1 :n 2}} {:a {:m 1}} {:a {:n 2}}
    {:a nil}         {:a {:m 1}} {:a nil}
    {:a {:m 1}}      {:a nil}    {:a {:m 1}}))
