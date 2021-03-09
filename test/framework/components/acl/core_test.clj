(ns framework.components.acl.core-test
  (:require
    [clojure.test :refer :all]
    [framework.components.acl.core :refer [has-access
                                           is-allowed]]))

(def custom-roles
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

(deftest custom-roles-test
  (is (= :all (has-access custom-roles {:role :customer :resource "items" :privilege :read})))
  (is (false? (has-access custom-roles {:role :customer :resource "items" :privilege :delete})))
  (is (= :own (has-access custom-roles {:role :customer :resource "users" :privilege :read})))
  (is (= :own (has-access custom-roles {:role :customer :resource "users" :privilege :delete})))
  (is (= :all (has-access custom-roles {:role :administrator :resource "users" :privilege :read}))))

(def default-roles
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

(deftest default-role-tests
  (is (= :all (has-access default-roles guest {:resource "items" :privilege :read})))
  (is (false? (has-access default-roles guest {:resource "users" :privilege :read})))
  (is (= :own (has-access default-roles member {:resource "users" :privilege :read})))
  (is (= :own (has-access default-roles member {:resource "addresses" :privilege :read})))
  (is (false? (has-access default-roles staff {:resource "users" :privilege :read})))
  (is (= :all (has-access default-roles staff {:resource "items" :privilege :update})))
  (is (= :all (has-access default-roles admin {:resource "items" :privilege :create})))
  (is (= :all (has-access default-roles admin {:resource "items" :privilege :create})))
  (is (= :all (has-access default-roles suspended-admin {:resource "items" :privilege :read})))
  (is (false? (has-access default-roles suspended-admin {:resource "items" :privilege :create})))
  (is (false? (has-access default-roles suspended-admin {:resource "users" :privilege :delete}))))

(defn state-with-user
  ([user]
   (if (map? user)
     {:acl/permissions default-roles
      :session         {:user user}}
     {:acl/permissions custom-roles
      :session         {:user {:role user}}})))

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
  (is (= :all
         (get-ok (is-allowed (state-with-user guest) {:resource "items" :privilege :read}))))
  (is (= {:status 401 :body "Authorization error"}
         (get-error (is-allowed (state-with-user guest) {:resource "items" :privilege :update}))))
  (is (= :own
         (get-ok (is-allowed (state-with-user member) {:resource "users" :privilege :update}))))
  (is (= :own
         (get-ok (is-allowed (state-with-user :customer) {:resource "users" :privilege :update}))))
  (is (= :all
         (get-ok (is-allowed (state-with-user :administrator) {:resource "users" :privilege :create}))))
  (is (= :all
         (get-ok (is-allowed (state-with-user :administrator) {:resource "users" :privilege :delete})))))

(defn state-with-user-request
  [user uri method]
  (if (map? user)
    {:acl/permissions default-roles
     :session         {:user user}
     :http-request    {:uri            uri
                       :request-method method}}
    {:acl/permissions custom-roles
     :session         {:user {:role user}}
     :http-request    {:uri            uri
                       :request-method method}}))

(deftest xiana-flow-from-request-only
  (is (= :all
         (get-ok (is-allowed (state-with-user-request guest "/items/" :get)))))
  (is (= {:status 401, :body "Authorization error"}
         (get-error (is-allowed (state-with-user-request guest "/items/" :post)))))
  (is (= :own
         (get-ok (is-allowed (state-with-user-request member "/addresses/" :post)))))
  (is (= :all
         (get-ok (is-allowed (state-with-user-request admin "/items/" :get)))))
  (is (= :all
         (get-ok (is-allowed (state-with-user-request admin "/items/" :put)))))
  (is (= :all
         (get-ok (is-allowed (state-with-user-request admin "/items/" :post)))))
  (is (= :all
         (get-ok (is-allowed (state-with-user-request suspended-admin "/items/" :get)))))
  (is (= {:status 401, :body "Authorization error"}
         (get-error (is-allowed (state-with-user-request suspended-admin "/items/" :put)))))
  (is (= :all
         (get-ok (is-allowed (state-with-user-request :customer "/items/" :get)))))
  (is (= {:status 401, :body "Authorization error"}
         (get-error (is-allowed (state-with-user-request :customer "/items/" :post)))))
  (is (= :own
         (get-ok (is-allowed (state-with-user-request :customer "/addresses/" :post)))))
  (is (= :all
         (get-ok (is-allowed (state-with-user-request :administrator "/items/" :get)))))
  (is (= :all
         (get-ok (is-allowed (state-with-user-request :administrator "/items/" :put)))))
  (is (= :all
         (get-ok (is-allowed (state-with-user-request :administrator "/items/" :post))))))

(def complex-roles
  {:guest     [{:resource "posts"
                :actions  [:read]
                :filter   :all}]
   :member    [{:resource "posts"
                :actions  [:read]
                :filter   :all}
               {:resource "posts"
                :actions  [:create :update :delete]
                :filter   :own}
               {:resource "comments"
                :actions  [:create :update :delete]
                :filter   :own}
               {:resource "comments"
                :actions  [:read]
                :filter   :all}
               {:resource "users"
                :actions  [:create :update :delete]
                :filter   :own}
               {:resource "users"
                :actions  [:read]
                :filter   :all}]
   :staff     [{:resource "posts"
                :actions  [:read :update :delete]
                :filter   :all}
               {:resource "comments"
                :actions  [:read :update :delete]
                :filter   :all}
               {:resource "users"
                :actions  [:read]
                :filter   :all}]
   :superuser [{:resource :all
                :actions  [:all]
                :filter   :all}]})

(deftest complex-roles-test
  (is (= :all (has-access complex-roles {:role :guest :resource "posts" :privilege :read})))
  (is (false? (has-access complex-roles {:role :guest :resource "posts" :privilege :create})))
  (is (= :all (has-access complex-roles {:role :member :resource "posts" :privilege :read})))
  (is (= :own (has-access complex-roles {:role :member :resource "posts" :privilege :create})))
  (is (= :all (has-access complex-roles {:role :member :resource "comments" :privilege :read})))
  (is (= :own (has-access complex-roles {:role :member :resource "comments" :privilege :update}))))
