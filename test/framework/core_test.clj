(ns framework.core-test
  (:require
    [clj-kondo.core :as kondo]
    [clojure.test :refer [deftest
                          is]]))

(deftest lint
  (let [{:keys [findings _analysis]}
        (kondo/run! {:lint ["src"]
                     :config {:linters {:not-empty?        false
                                        :unresolved-symbol {:exclude '[get-bases get-bases:sm get-bases:md
                                                                       get-bases:lg get-bases:xl get-bases:2xl
                                                                       get-theme get-container get-animation
                                                                       default-components get-hiccup-classes
                                                                       spit]}}

                              :output        {:analysis true}
                              :lint-as       `{}
                              :skip-comments true}})]
    (when-not (empty? findings)
      (clojure.pprint/pprint findings))
    (is (empty? findings))))
