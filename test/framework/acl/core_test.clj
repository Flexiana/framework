(ns framework.acl.core-test
  (:require
    [clojure.test :refer :all]
    [framework.acl.core :refer [is-allowed]]))

(def custom-roles
  {:customer         [{:resource "items"
                       :actions  [:read]
                       :over     :all}
                      {:resource "users"
                       :actions  [:read :update :delete]
                       :over     :own}
                      {:resource "addresses"
                       :actions  [:create :read :update :delete]
                       :over     :own}
                      {:resource "carts"
                       :actions  [:create :read :update :delete]
                       :over     :own}]
   :warehouse-worker [{:resource "items"
                       :actions  [:read :update]
                       :over     :all}]
   :postal-worker    [{:resource "carts"
                       :actions  [:read :update]
                       :over     :all}
                      {:resource "addresses"
                       :actions  [:read]
                       :over     :all}]
   :shop-worker      [{:resource "items"
                       :actions  [:all]
                       :over     :all}]
   :administrator    [{:resource :all
                       :actions  [:all]
                       :over     :all}]})

(def default-roles
  {:guest     [{:resource "items"
                :actions  [:read]
                :over     :all}]
   :member    [{:resource "items"
                :actions  [:read]
                :over     :all}
               {:resource "users"
                :actions  [:read :update :delete]
                :over     :own}
               {:resource "addresses"
                :actions  [:create :read :update :delete]
                :over     :own}
               {:resource "carts"
                :actions  [:create :read :update :delete]
                :over     :own}]
   :staff     [{:resource "items"
                :actions  [:read :update]
                :over     :all}]
   :superuser [{:resource :all
                :actions  [:all]
                :over     :all}]})

(def guest {})
(def member {:is_active true})
(def admin {:is_active true :is_superuser true})
(def suspended-admin {:is_active false :is_superuser true})

(defn state-with-user
  ([user]
   (if (map? user)
     {:acl/roles    default-roles
      :session-data {:user user}}
     {:acl/roles    custom-roles
      :session-data {:user {:role user}}})))

(defn get-ok
  [t]
  (get-in t [:right :response-data :acl]))

(defn get-error
  [t]
  (get-in t [:left :response]))

(deftest xiana-flow-with-access-map
  (is (= {:status 401 :body "Authorization error"}
         (get-error (is-allowed (state-with-user guest) {:resource "items" :privilege :create}))))
  (is (= "Not ok 401"
         (is-allowed (state-with-user guest) {:resource "items" :privilege :create :or-else (fn [_] (str "Not ok " 401))})))
  (is (= {:items :all}
         (get-ok (is-allowed (state-with-user guest) {:resource "items" :privilege :read}))))
  (is (= {:status 401 :body "Authorization error"}
         (get-error (is-allowed (state-with-user guest) {:resource "items" :privilege :update}))))
  (is (= {:users :own}
         (get-ok (is-allowed (state-with-user member) {:resource "users" :privilege :update}))))
  (is (= {:users :own}
         (get-ok (is-allowed (state-with-user :customer) {:resource "users" :privilege :update}))))
  (is (= {:users :all}
         (get-ok (is-allowed (state-with-user :administrator) {:resource "users" :privilege :create}))))
  (is (= {:users :all}
         (get-ok (is-allowed (state-with-user :administrator) {:resource "users" :privilege :delete})))))

(defn state-with-user-request
  [user uri method]
  (if (map? user)
    {:acl/roles    default-roles
     :session-data {:user user}
     :request      {:uri            uri
                    :request-method method}}
    {:acl/roles    custom-roles
     :session-data {:user {:role user}}
     :request      {:uri            uri
                    :request-method method}}))

(deftest xiana-flow-from-request-only
  (is (= {:items :all}
         (get-ok (is-allowed (state-with-user-request guest "/items/" :get)))))
  (is (= {:status 401, :body "Authorization error"}
         (get-error (is-allowed (state-with-user-request guest "/items/" :post)))))
  (is (= {:addresses :own}
         (get-ok (is-allowed (state-with-user-request member "/addresses/" :post)))))
  (is (= {:items :all}
         (get-ok (is-allowed (state-with-user-request admin "/items/" :get)))))
  (is (= {:items :all}
         (get-ok (is-allowed (state-with-user-request admin "/items/" :put)))))
  (is (= {:items :all}
         (get-ok (is-allowed (state-with-user-request admin "/items/" :post)))))
  (is (= {:items :all}
         (get-ok (is-allowed (state-with-user-request suspended-admin "/items/" :get)))))
  (is (= {:status 401, :body "Authorization error"}
         (get-error (is-allowed (state-with-user-request suspended-admin "/items/" :put)))))
  (is (= {:items :all}
         (get-ok (is-allowed (state-with-user-request :customer "/items/" :get)))))
  (is (= {:status 401, :body "Authorization error"}
         (get-error (is-allowed (state-with-user-request :customer "/items/" :post)))))
  (is (= {:addresses :own}
         (get-ok (is-allowed (state-with-user-request :customer "/addresses/" :post)))))
  (is (= {:items :all}
         (get-ok (is-allowed (state-with-user-request :administrator "/items/" :get)))))
  (is (= {:items :all}
         (get-ok (is-allowed (state-with-user-request :administrator "/items/" :put)))))
  (is (= {:items :all}
         (get-ok (is-allowed (state-with-user-request :administrator "/items/" :post))))))
