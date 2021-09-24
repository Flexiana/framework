(ns framework.acl.builder.builder-functions-test
  (:require
    [clojure.test :refer :all]
    [framework.acl.builder.builder-functions :refer [add-actions
                                                     override-actions
                                                     remove-resource
                                                     allow
                                                     deny
                                                     revoke
                                                     grant]]))


(deftest build-roles-allow
  (is (= {:guest [{:resource "posts", :actions [:read], :over :all}]}
         (allow {} {:role :guest :resource "posts" :actions :read :over :all})))

  (is (= {:guest [{:resource "posts", :actions [:response :read], :over :all}]}
         (allow {} {:role :guest :resource "posts" :actions [:response :read] :over :all})))

  (is (= {:guest [{:resource "posts", :actions [:read], :over :own}]}
         (-> (allow {} {:role :guest :resource "posts" :actions :read :over :all})
             (allow {:role :guest :resource "posts" :actions :read :over :own}))))
  (is (nil?
        (revoke {:role :guest :resource "posts" :actions [:delete] :over :own} :delete)))
  (is (= {:role :guest, :resource "posts", :actions [:reply], :over :own}
         (revoke {:role :guest :resource "posts" :actions [:delete :reply] :over :own} :delete)))
  (is (= {:role :guest, :resource "posts", :actions [:reply :delete], :over :own}
         (grant {:role :guest, :resource "posts", :actions [:reply], :over :own} :delete)))
  (is (= {:guest [{:resource "posts", :actions [:delete], :over :all}]}
         (-> (allow {} {:role :guest :resource "posts" :actions [:delete] :over :own})
             (allow {:role :guest :resource "posts" :actions [:delete]}))))
  (is (= {:guest [{:resource "posts", :actions [:read], :over :all}
                  {:resource "posts", :actions [:delete], :over :own}]}
         (-> (allow {} {:role :guest :resource "posts" :actions [:read :delete] :over :own})
             (allow {:role :guest :resource "posts" :actions [:read]}))))
  (is (= {:guest [{:resource "posts", :actions [:all], :over :all}]}
         (-> (allow {} {:role :guest :resource "posts" :actions :all :over :all})
             (allow {:role :guest :resource "posts" :actions [:response] :over :own})
             (allow {:role :guest :resource "posts" :actions [:delete] :over :own})
             (allow {:role :guest :resource "posts" :actions [:delete]}))))
  (is (= {:guest [{:resource "posts", :actions [:all], :over :own}]}
         (-> (allow {} {:role :guest :resource "posts" :actions :all :over :all})
             (allow {:role :guest :resource "posts" :actions :all :over :own}))))
  (is (= {:guest [{:resource "posts", :actions [:all], :over :all}]}
         (-> (allow {} {:role :guest :resource "posts" :actions :all :over :own})
             (allow {:role :guest :resource "posts" :actions :all :over :all}))))
  (is (= {:guest [{:resource "posts", :actions [:response :delete], :over :own}
                  {:resource "posts", :actions [:read], :over :all}]}
         (-> (allow {} {:role :guest :resource "posts" :actions :read :over :all})
             (allow {:role :guest :resource "posts" :actions [:response] :over :own})
             (allow {:role :guest :resource "posts" :actions [:delete] :over :own}))))
  (is (= {:guest [{:resource "posts", :actions [:all], :over :all}]}
         (-> (allow {} {:role :guest :resource "posts" :actions :read :over :all})
             (allow {:role :guest :resource "posts" :actions [:response] :over :own})
             (allow {:role :guest :resource "posts" :actions [:delete] :over :own})
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
  (is (empty? (deny {:guest [{:resource "posts", :actions [:response :delete], :over :own}
                             {:resource "posts", :actions [:read], :over :all}]}
                    {"comment" [:read :delete], "post" [:read :delete :update :comment]}
                    {:role :guest :resource "posts", :actions :all})))
  (is (= {:guest [{:resource "posts", :actions [:response :delete], :over :own}]}
         (deny {:guest [{:resource "posts", :actions [:response :delete], :over :own}
                        {:resource "posts", :actions [:read], :over :all}]}
               {"comment" [:read :delete], "post" [:read :delete :update :comment]}
               {:role :guest :resource "posts", :actions :read})))
  (is (= {:guest [{:resource "posts", :actions [:response :delete], :over :own}]}
         (deny {:guest [{:resource "posts", :actions [:response :delete], :over :own}
                        {:resource "comments", :actions [:read], :over :all}]}
               {"comment" [:read :delete], "post" [:read :delete :update :comment]}
               {:role :guest :resource "comments", :actions :read})))
  (is (= {:guest [{:resource "comments", :actions [:delete], :over :own}
                  {:resource "comments", :actions [:delete :like], :over :all}]}
         (deny {:guest [{:resource "comments", :actions [:all], :over :all}
                        {:resource "comments", :actions [:read :delete], :over :own}]}
               {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
               {:role :guest :resource "comments", :actions :read})))
  (is (= {:guest [{:resource "posts", :actions [:all], :over :own}
                  {:resource "comments", :actions [:delete], :over :own}
                  {:resource "comments", :actions [:delete :like], :over :all}]}
         (deny {:guest [{:resource "comments", :actions [:all], :over :all}
                        {:resource "comments", :actions [:read :delete], :over :own}
                        {:resource "posts", :actions [:all], :over :own}]}
               {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
               {:role :guest :resource "comments", :actions :read})))
  (is (empty? (deny {:guest [{:resource :all, :actions [:read], :over :all}]}
                    {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
                    {:role :guest :resource :all, :actions :all})))
  (is (= {:guest [{:resource :all, :actions [:delete], :over :all}
                  {:resource "post", :actions [:delete], :over :all}]}
         (deny {:guest [{:resource :all, :actions [:read :delete], :over :all}
                        {:resource "post", :actions [:read :delete], :over :all}]}
               {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
               {:role :guest :resource :all, :actions :read})))
  (is (= {:guest [{:resource "comments", :actions [:read], :over :all}
                  {:resource "post", :actions [:read], :over :all}]}
         (deny {:guest [{:resource "comments", :actions [:read :delete], :over :all}
                        {:resource "post", :actions [:read :delete], :over :all}]}
               {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
               {:role :guest :resource :all, :actions :delete})))
  (is (= {:guest [{:resource "comments", :actions [:read], :over :all}]}
         (deny {:guest [{:resource "comments", :actions [:read :delete], :over :all}
                        {:resource "post", :actions [:all], :over :all}]}
               {}
               {:role :guest :resource :all, :actions :delete}))))


(deftest build-roles-allow-with-available-permissions
  (is (= {:guest [{:resource "comments", :actions [:read], :over :all}]}
         (allow {}
                {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
                {:role :guest :resource "comments", :actions :read})))
  (is (= {:guest [{:resource "comments", :actions [:read :delete], :over :all}]}
         (allow {}
                {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
                {:role :guest :resource "comments", :actions [:read :delete]})))
  (is (= {:guest [{:resource "comments", :actions [:all], :over :all}]}
         (allow {}
                {"comments" [:read :delete :like], "post" [:read :delete :update :comment]}
                {:role :guest :resource "comments", :actions [:read :delete :like]}))))


