(ns framework.components.acl.core-test
  (:require
    [clojure.test :refer :all]
    [framework.components.acl.core :refer [isAllowed]]))

(def config
  {:users/permissions {:customer         [{:resource "items"
                                           :actions  [:read]
                                           :filter   :all}
                                          {:resource "users"
                                           :actions  [:read :update :delete]
                                           :filter   :own}
                                          {:resource "addresses"
                                           :actions  [:create :read :update :delete]
                                           :filter   :own}
                                          {:resource "carts"
                                           :actions  [:create :read :update :delete]
                                           :filter   :own}]
                       :warehouse-worker [{:resource "items"
                                           :actions  [:read :update]
                                           :filter   :all}]
                       :postal-worker    [{:resource "carts"
                                           :actions  [:read :update]
                                           :filter   :all}
                                          {:resource "addresses"
                                           :actions  [:read]
                                           :filter   :all}]
                       :shop-worker      [{:resource "items"
                                           :actions  [:all]
                                           :filter   :all}]
                       :administrator    [{:resource :all
                                           :actions  [:all]
                                           :filter   :all}]}})

(deftest isAllowed-test
  (is (= :all (isAllowed config {:role :customer :resource "items" :privilege :read})))
  (is (false? (isAllowed config {:role :customer :resource "items" :privilege :delete})))
  (is (= :own (isAllowed config {:role :customer :resource "users" :privilege :read})))
  (is (= :own (isAllowed config {:role :customer :resource "users" :privilege :delete})))
  (is (= :all (isAllowed config {:role :administrator :resource "users" :privilege :read}))))
