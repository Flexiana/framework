(ns framework.core-test
  (:require
    [clj-kondo.core :as kondo]
    [clojure.test :refer [deftest
                          is]]))

(deftest lint
  (let [{:keys [findings _analysis]}
        (kondo/run! {:lint ["src"]
                     :config {:linters {:not-empty?        false
                                        :unresolved-symbol {:exclude '[]}}
                              :output        {:analysis true}
                              :lint-as       `{}
                              :skip-comments true}})]
    (is (empty? findings))))
