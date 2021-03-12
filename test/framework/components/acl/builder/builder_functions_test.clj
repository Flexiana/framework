(ns framework.components.acl.builder.builder-functions-test
  (:require
    [clojure.test :refer :all]
    [framework.components.acl.builder.builder-functions :refer [add-actions
                                                                override-actions
                                                                remove-resource
                                                                allow
                                                                deny
                                                                revoke
                                                                grant]]))

(deftest build-roles-allow
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

(deftest build-permissions
  (is (= {"comment" [:delete :read], "post" [:comment :update :delete :read]}
         (-> (add-actions {} {"comment" :read
                              "post"    [:read :delete :update :comment]})
             (add-actions {"comment" :delete}))))
  (is (= {"comment" [:delete], "post" [:comment :update :delete :read]}
         (-> (add-actions {} {"comment" :read
                              "post"    [:read :delete :update :comment]})
             (override-actions {"comment" :delete}))))
  (is (= {"comment" [:delete :read]}
         (-> (add-actions {} {"comment" :read
                              "post"    [:read :delete :update :comment]})
             (add-actions {"comment" :delete})
             (remove-resource "post")))))

(deftest build-roles-deny
  (is (empty? (deny {:guest [{:resource "posts", :actions [:response :delete], :restriction :own}
                             {:resource "posts", :actions [:read], :restriction :all}]}
                    {"comment" [:read :delete], "post" [:read :delete :update :comment]}
                    {:role :guest :resource "posts", :actions :all})))
  (is (= {:guest [{:resource "posts", :actions [:response :delete], :restriction :own}]}
         (deny {:guest [{:resource "posts", :actions [:response :delete], :restriction :own}
                        {:resource "posts", :actions [:read], :restriction :all}]}
               {"comment" [:read :delete], "post" [:read :delete :update :comment]}
               {:role :guest :resource "posts", :actions :read})))
  (is (= {:guest [{:resource "posts", :actions [:response :delete], :restriction :own}]}
         (deny {:guest [{:resource "posts", :actions [:response :delete], :restriction :own}
                        {:resource "comments", :actions [:read], :restriction :all}]}
               {"comment" [:read :delete], "post" [:read :delete :update :comment]}
               {:role :guest :resource "comments", :actions :read})))
  (is (= {:guest [{:resource "comments", :actions [:delete], :restriction :own}
                  {:resource "comments", :actions [:delete :like], :restriction :all}]}
         (deny {:guest [{:resource "comments", :actions [:all], :restriction :all}
                        {:resource "comments", :actions [:read :delete], :restriction :own}]}
               {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
               {:role :guest :resource "comments", :actions :read})))
  (is (= {:guest [{:resource "posts", :actions [:all], :restriction :own}
                  {:resource "comments", :actions [:delete], :restriction :own}
                  {:resource "comments", :actions [:delete :like], :restriction :all}]}
         (deny {:guest [{:resource "comments", :actions [:all], :restriction :all}
                        {:resource "comments", :actions [:read :delete], :restriction :own}
                        {:resource "posts", :actions [:all], :restriction :own}]}
               {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
               {:role :guest :resource "comments", :actions :read})))
  (is (empty? (deny {:guest [{:resource :all, :actions [:read], :restriction :all}]}
                    {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
                    {:role :guest :resource :all, :actions :all})))
  (is (= {:guest [{:resource :all, :actions [:delete], :restriction :all}
                  {:resource "post", :actions [:delete], :restriction :all}]}
         (deny {:guest [{:resource :all, :actions [:read :delete], :restriction :all}
                        {:resource "post", :actions [:read :delete], :restriction :all}]}
               {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
               {:role :guest :resource :all, :actions :read})))
  (is (= {:guest [{:resource "comments", :actions [:read], :restriction :all}
                  {:resource "post", :actions [:read], :restriction :all}]}
         (deny {:guest [{:resource "comments", :actions [:read :delete], :restriction :all}
                        {:resource "post", :actions [:read :delete], :restriction :all}]}
               {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
               {:role :guest :resource :all, :actions :delete})))
  (is (= {:guest [{:resource "comments", :actions [:read], :restriction :all}]}
         (deny {:guest [{:resource "comments", :actions [:read :delete], :restriction :all}
                        {:resource "post", :actions [:all], :restriction :all}]}
               {}
               {:role :guest :resource :all, :actions :delete}))))

(deftest build-roles-allow-with-available-permissions
  (is (= {:guest [{:resource "comments", :actions [:read], :restriction :all}]}
         (allow {}
                {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
                {:role :guest :resource "comments", :actions :read})))
  (is (= {:guest [{:resource "comments", :actions [:read :delete], :restriction :all}]}
         (allow {}
                {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
                {:role :guest :resource "comments", :actions [:read :delete]})))
  (is (= {:guest [{:resource "comments", :actions [:all], :restriction :all}]}
         (allow {}
                {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
                {:role :guest :resource "comments", :actions [:read :delete :like]}))))


