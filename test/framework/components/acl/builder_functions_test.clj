(ns framework.components.acl.builder-functions-test
  (:require
    [clojure.test :refer :all]
    [framework.components.acl.builder-functions :refer [add-actions
                                                        override-actions
                                                        remove-resource
                                                        allow
                                                        deny
                                                        revoke
                                                        grant]]))

(deftest build-config-allow
  (is (= {:guest [{:resource "posts", :actions [:read], :restriction :all}]}
         (allow {} {:role :guest :resource "posts" :actions :read :restriction :all})))

  (is (= {:guest [{:resource "posts", :actions [:response :read], :restriction :all}]}
         (allow {} {:role :guest :resource "posts" :actions [:response :read] :restriction :all})))

  (is (= {:guest [{:resource "posts", :actions [:read], :restriction :own}]}
         (-> (allow {} {:role :guest :resource "posts" :actions :read :restriction :all})
             (allow {:role :guest :resource "posts" :actions :read :restriction :own}))))
  (is (nil?
        (revoke {:role :guest :resource "posts" :actions [:delete] :restriction :own} :delete)))
  (is (= {:role :guest, :resource "posts", :actions [:reply], :restriction :own}
         (revoke {:role :guest :resource "posts" :actions [:delete :reply] :restriction :own} :delete)))
  (is (= {:role :guest, :resource "posts", :actions [:reply :delete], :restriction :own}
         (grant {:role :guest, :resource "posts", :actions [:reply], :restriction :own} :delete)))
  (is (= {:guest [{:resource "posts", :actions [:delete], :restriction :all}]}
         (-> (allow {} {:role :guest :resource "posts" :actions [:delete] :restriction :own})
             (allow {:role :guest :resource "posts" :actions [:delete]}))))
  (is (= {:guest [{:resource "posts", :actions [:read], :restriction :all}
                  {:resource "posts", :actions [:delete], :restriction :own}]}
         (-> (allow {} {:role :guest :resource "posts" :actions [:read :delete] :restriction :own})
             (allow {:role :guest :resource "posts" :actions [:read]}))))
  (is (= {:guest [{:resource "posts", :actions [:all], :restriction :all}]}
         (-> (allow {} {:role :guest :resource "posts" :actions :all :restriction :all})
             (allow {:role :guest :resource "posts" :actions [:response] :restriction :own})
             (allow {:role :guest :resource "posts" :actions [:delete] :restriction :own})
             (allow {:role :guest :resource "posts" :actions [:delete]}))))
  (is (= {:guest [{:resource "posts", :actions [:all], :restriction :own}]}
         (-> (allow {} {:role :guest :resource "posts" :actions :all :restriction :all})
             (allow {:role :guest :resource "posts" :actions :all :restriction :own}))))
  (is (= {:guest [{:resource "posts", :actions [:all], :restriction :all}]}
         (-> (allow {} {:role :guest :resource "posts" :actions :all :restriction :own})
             (allow {:role :guest :resource "posts" :actions :all :restriction :all}))))
  (is (= {:guest [{:resource "posts", :actions [:response :delete], :restriction :own}
                  {:resource "posts", :actions [:read], :restriction :all}]}
         (-> (allow {} {:role :guest :resource "posts" :actions :read :restriction :all})
             (allow {:role :guest :resource "posts" :actions [:response] :restriction :own})
             (allow {:role :guest :resource "posts" :actions [:delete] :restriction :own}))))
  (is (= {:guest [{:resource "posts", :actions [:all], :restriction :all}]}
         (-> (allow {} {:role :guest :resource "posts" :actions :read :restriction :all})
             (allow {:role :guest :resource "posts" :actions [:response] :restriction :own})
             (allow {:role :guest :resource "posts" :actions [:delete] :restriction :own})
             (allow {:role :guest :resource "posts" :actions [:all]})))))

(deftest build-available-permissions
  (is (= {"comment" [:read :delete], "post" [:read :delete :update :comment]}
         (-> (add-actions {} {"comment" :read
                              "post"    [:read :delete :update :comment]})
             (add-actions {"comment" :delete}))))
  (is (= {"comment" :delete, "post" [:read :delete :update :comment]}
         (-> (add-actions {} {"comment" :read
                              "post"    [:read :delete :update :comment]})
             (override-actions {"comment" :delete}))))
  (is (= {"comment" [:read :delete]}
         (-> (add-actions {} {"comment" :read
                              "post"    [:read :delete :update :comment]})
             (add-actions {"comment" :delete})
             (remove-resource "post")))))

(deftest build-config-deny
  (is (empty? (deny {"comment" [:read :delete], "post" [:read :delete :update :comment]}
                    {:guest [{:resource "posts", :actions [:response :delete], :restriction :own}
                             {:resource "posts", :actions [:read], :restriction :all}]}
                    {:role :guest :resource "posts", :actions :all})))
  (is (= {:guest [{:resource "posts", :actions [:response :delete], :restriction :own}]}
         (deny {"comment" [:read :delete], "post" [:read :delete :update :comment]}
               {:guest [{:resource "posts", :actions [:response :delete], :restriction :own}
                        {:resource "posts", :actions [:read], :restriction :all}]}
               {:role :guest :resource "posts", :actions :read})))
  (is (= {:guest [{:resource "posts", :actions [:response :delete], :restriction :own}]}
         (deny {"comment" [:read :delete], "post" [:read :delete :update :comment]}
               {:guest [{:resource "posts", :actions [:response :delete], :restriction :own}
                        {:resource "comments", :actions [:read], :restriction :all}]}
               {:role :guest :resource "comments", :actions :read})))
  (is (= {:guest [{:resource "comments", :actions [:delete], :restriction :own}
                  {:resource "comments", :actions [:delete :like], :restriction :all}]}
         (deny {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
               {:guest [{:resource "comments", :actions [:all], :restriction :all}
                        {:resource "comments", :actions [:read :delete], :restriction :own}]}
               {:role :guest :resource "comments", :actions :read})))
  (is (= {:guest [{:resource "posts", :actions [:all], :restriction :own}
                  {:resource "comments", :actions [:delete], :restriction :own}
                  {:resource "comments", :actions [:delete :like], :restriction :all}]}
         (deny {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
               {:guest [{:resource "comments", :actions [:all], :restriction :all}
                        {:resource "comments", :actions [:read :delete], :restriction :own}
                        {:resource "posts", :actions [:all], :restriction :own}]}
               {:role :guest :resource "comments", :actions :read})))
  (is (empty? (deny {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
                    {:guest [{:resource :all, :actions [:read], :restriction :all}]}
                    {:role :guest :resource :all, :actions :all})))
  (is (= {:guest [{:resource :all, :actions [:delete], :restriction :all}
                  {:resource "post", :actions [:delete], :restriction :all}]}
         (deny {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
               {:guest [{:resource :all, :actions [:read :delete], :restriction :all}
                        {:resource "post", :actions [:read :delete], :restriction :all}]}
               {:role :guest :resource :all, :actions :read})))
  (is (= {:guest [{:resource "comments", :actions [:read], :restriction :all}
                  {:resource "post", :actions [:read], :restriction :all}]}
         (deny {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
               {:guest [{:resource "comments", :actions [:read :delete], :restriction :all}
                        {:resource "post", :actions [:read :delete], :restriction :all}]}
               {:role :guest :resource :all, :actions :delete}))))
