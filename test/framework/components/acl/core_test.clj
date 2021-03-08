(ns framework.components.acl.core-test
  (:require
    [clojure.test :refer :all]
    [framework.components.acl.core :refer [isAllowed]]))

(def config
  {:customer         [{:resource "items"
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
                       :filter   :all}]})

(deftest isAllowed-test
  (is (= :all (isAllowed config {:role :customer :resource "items" :privilege :read})))
  (is (false? (isAllowed config {:role :customer :resource "items" :privilege :delete})))
  (is (= :own (isAllowed config {:role :customer :resource "users" :privilege :read})))
  (is (= :own (isAllowed config {:role :customer :resource "users" :privilege :delete})))
  (is (= :all (isAllowed config {:role :administrator :resource "users" :privilege :read}))))

(def config-for-user-based-ussage
  {:guest     [{:resource "items"
                :actions  [:read]
                :filter   :all}]
   :member    [{:resource "items"
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
   :staff     [{:resource "items"
                :actions  [:read :update]
                :filter   :all}]
   :superuser [{:resource :all
                :actions  [:all]
                :filter   :all}]})

(def guest {})
(def member {:is_active true})
(def staff {:is_active true :is_staff true})
(def admin {:is_active true :is_superuser true})
(def suspended-admin {:is_active false :is_superuser true})

(deftest user-based-acl
  (is (= :all (isAllowed config-for-user-based-ussage guest {:resource "items" :privilege :read})))
  (is (false? (isAllowed config-for-user-based-ussage guest {:resource "users" :privilege :read})))
  (is (= :own (isAllowed config-for-user-based-ussage member {:resource "users" :privilege :read})))
  (is (= :own (isAllowed config-for-user-based-ussage member {:resource "addresses" :privilege :read})))
  (is (false? (isAllowed config-for-user-based-ussage staff {:resource "users" :privilege :read})))
  (is (= :all (isAllowed config-for-user-based-ussage staff {:resource "items" :privilege :update})))
  (is (= :all (isAllowed config-for-user-based-ussage admin {:resource "items" :privilege :create})))
  (is (= :all (isAllowed config-for-user-based-ussage admin {:resource "items" :privilege :create})))
  (is (= :all (isAllowed config-for-user-based-ussage suspended-admin {:resource "items" :privilege :read})))
  (is (false? (isAllowed config-for-user-based-ussage suspended-admin {:resource "items" :privilege :create})))
  (is (false? (isAllowed config-for-user-based-ussage suspended-admin {:resource "users" :privilege :delete}))))
