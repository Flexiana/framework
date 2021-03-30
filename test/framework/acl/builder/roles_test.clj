(ns framework.acl.builder.roles-test
  (:require
    [clojure.test :refer :all]
    [framework.acl.builder.permissions :as p]
    [framework.acl.builder.roles :refer [allow
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
  (test-roles {:guest [{:resource "posts", :actions [:blow], :over :own}
                       {:resource "posts", :actions [:read :delete], :over :all}]}
              (xiana/flow->
                {}
                init
                (allow {:role :guest :resource "posts" :actions :read})
                (allow {:role :guest :resource "posts" :actions :delete})
                (allow {:role :guest :resource "posts" :actions :blow :over :own})))
  (test-roles {:guest [{:resource "posts", :actions [:read :delete], :over :all}]}
              (xiana/flow->
                {}
                (p/init {"posts" [:read :delete :blow :blow-up]})
                (init {:guest [{:resource "posts", :actions [:blow], :over :own}
                               {:resource "posts", :actions [:read :delete], :over :all}]})
                (deny {:role :guest :resource "posts" :actions :blow})))
  (test-roles {}
              (xiana/flow->
                {}
                (p/init {"posts" [:read :delete :blow :blow-up]})
                (init {:guest [{:resource "posts", :actions [:blow], :over :own}
                               {:resource "posts", :actions [:read :delete], :over :all}]})
                (deny {:role :guest :resource "posts" :actions :all})))
  (test-roles {:guest [{:resource "posts", :actions [:read :blow :blow-up], :over :own}]}
              (xiana/flow->
                {}
                (p/init {"posts" [:read :delete :blow :blow-up]})
                (init {:guest [{:resource "posts", :actions [:all], :over :own}]})
                (deny {:role :guest :resource "posts" :actions :delete})))
  (test-roles {:guest  [{:resource "posts", :actions [:read], :over :own}]
               :member [{:resource "posts", :actions [:delete :read], :over :own}]}
              (xiana/flow->
                {}
                ;(p/init {"posts" [:read :delete :blow :blow-up]})
                (init {:guest  [{:resource "posts", :actions [:delete :read], :over :own}]
                       :member [{:resource "posts", :actions [:delete :read], :over :own}]})
                (deny {:role :guest :resource "posts" :actions :delete})))
  (test-roles {:member [{:resource "posts", :actions [:delete :read], :over :own}]}
              (xiana/flow->
                {}
                ;(p/init {"posts" [:read :delete :blow :blow-up]})
                (init {:guest  [{:resource "posts", :actions [:all], :over :own}]
                       :member [{:resource "posts", :actions [:delete :read], :over :own}]})
                (deny {:role :guest :resource "posts" :actions :delete})))
  (test-roles {:guest  [{:resource "posts", :actions [:read :blow :blow-up], :over :own}]
               :member [{:resource "posts", :actions [:delete :read], :over :own}]}
              (xiana/flow->
                {}
                (p/init {"posts" [:read :delete :blow :blow-up]})
                (init {:guest  [{:resource "posts", :actions [:all], :over :own}]
                       :member [{:resource "posts", :actions [:delete :read], :over :own}]})
                (deny {:role :guest :resource "posts" :actions :delete}))))
