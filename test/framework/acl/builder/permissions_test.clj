(ns framework.acl.builder.permissions-test
  (:require
    [clojure.test :refer :all]
    [framework.acl.builder.permissions :refer [init add-actions
                                               override-actions
                                               remove-resource]]
    [xiana.core :as xiana]))


(defn test-permissions
  [expected actual]
  (is (= expected (-> actual
                      :right
                      :acl/available-permissions))))


(deftest permissions-builder
  (test-permissions {"comments" [:read]}
                    (xiana/flow->
                      {}
                      (init {"comments" [:read]})))
  (test-permissions {"comments" [:read]}
                    (xiana/flow->
                      {}
                      (init {"comments" :read})))
  (test-permissions {"posts" [:send :update :delete :read], "comments" [:send]}
                    (xiana/flow->
                      {}
                      init
                      (add-actions {"posts" :read})
                      (add-actions {"posts" :delete})
                      (add-actions {"posts" :update})
                      (add-actions {"posts" :send})
                      (add-actions {"comments" :send})
                      (add-actions {"comments" :send})
                      (add-actions {"comments" :send})))
  (test-permissions {"comments" [:send]}
                    (xiana/flow->
                      {}
                      init
                      (add-actions {"posts" :read})
                      (add-actions {"posts" :delete})
                      (add-actions {"posts" :update})
                      (add-actions {"posts" :send})
                      (add-actions {"comments" :send})
                      (add-actions {"comments" :send})
                      (add-actions {"comments" :send})
                      (remove-resource "posts")))
  (test-permissions {"posts" [:blow-up], "comments" [:send]}
                    (xiana/flow->
                      {}
                      init
                      (add-actions {"posts" :read})
                      (add-actions {"posts" :delete})
                      (add-actions {"posts" :update})
                      (add-actions {"posts" :send})
                      (add-actions {"comments" :send})
                      (add-actions {"comments" :send})
                      (add-actions {"comments" :send})
                      (override-actions {"posts" :blow-up}))))
