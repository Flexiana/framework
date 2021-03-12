(ns framework.components.acl.builder.roles-test
  (:require
    [clojure.test :refer :all]
    [framework.components.acl.builder.permissions :as p]
    [framework.components.acl.builder.roles :refer [allow
                                                    deny
                                                    init]]
    [xiana.core :as xiana]))

(defn test-roles
  [expected actual]
  (is (= expected (-> actual
                      :right
                      :acl/roles))))

(deftest init-test
  (test-roles {}
              (xiana/flow->
                {}
                init))
  (test-roles {:guest [{:resource "posts", :actions [:blow], :restriction :own}
                       {:resource "posts", :actions [:read :delete], :restriction :all}]}
              (xiana/flow->
                {}
                init
                (allow {:role :guest :resource "posts" :actions :read})
                (allow {:role :guest :resource "posts" :actions :delete})
                (allow {:role :guest :resource "posts" :actions :blow :restriction :own})))
  (test-roles {:guest [{:resource "posts", :actions [:read :delete], :restriction :all}]}
              (xiana/flow->
                {}
                (p/init {"posts" [:read :delete :blow :blow-up]})
                (init {:guest [{:resource "posts", :actions [:blow], :restriction :own}
                               {:resource "posts", :actions [:read :delete], :restriction :all}]})
                (deny {:role :guest :resource "posts" :actions :blow})))
  (test-roles {}
              (xiana/flow->
                {}
                (p/init {"posts" [:read :delete :blow :blow-up]})
                (init {:guest [{:resource "posts", :actions [:blow], :restriction :own}
                               {:resource "posts", :actions [:read :delete], :restriction :all}]})
                (deny {:role :guest :resource "posts" :actions :all})))
  (test-roles {:guest [{:resource "posts", :actions [:read :blow :blow-up], :restriction :own}]}
              (xiana/flow->
                {}
                (p/init {"posts" [:read :delete :blow :blow-up]})
                (init {:guest [{:resource "posts", :actions [:all], :restriction :own}]})
                (deny {:role :guest :resource "posts" :actions :delete})))
  (test-roles {:guest [{:resource "posts", :actions [:read], :restriction :own}]
               :member [{:resource "posts", :actions [:delete :read], :restriction :own}]}
              (xiana/flow->
                {}
                ;(p/init {"posts" [:read :delete :blow :blow-up]})
                (init {:guest  [{:resource "posts", :actions [:delete :read], :restriction :own}]
                       :member [{:resource "posts", :actions [:delete :read], :restriction :own}]})
                (deny {:role :guest :resource "posts" :actions :delete})))
  (test-roles {:member [{:resource "posts", :actions [:delete :read], :restriction :own}]}
              (xiana/flow->
                {}
                ;(p/init {"posts" [:read :delete :blow :blow-up]})
                (init {:guest  [{:resource "posts", :actions [:all], :restriction :own}]
                       :member [{:resource "posts", :actions [:delete :read], :restriction :own}]})
                (deny {:role :guest :resource "posts" :actions :delete})))
  (test-roles {:guest  [{:resource "posts", :actions [:read :blow :blow-up], :restriction :own}]
               :member [{:resource "posts", :actions [:delete :read], :restriction :own}]}
              (xiana/flow->
                {}
                (p/init {"posts" [:read :delete :blow :blow-up]})
                (init {:guest  [{:resource "posts", :actions [:all], :restriction :own}]
                       :member [{:resource "posts", :actions [:delete :read], :restriction :own}]})
                (deny {:role :guest :resource "posts" :actions :delete}))))
