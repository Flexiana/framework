(ns framework.core-test
  (:require
   [clojure.test :refer [deftest
                         is
                         testing]]
   [clj-kondo.core :as kondo]))

(deftest lint
  (let [{:keys [findings analysis]}
        (kondo/run! {:lint ["src"]
                     :config {:linters {:not-empty?        false
                                        :unresolved-symbol {:exclude '[]}}
                              :output        {:analysis true}
                              :lint-as       `{}
                              :skip-comments true}})]
    (is (empty? findings))))
