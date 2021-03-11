(ns framework.components.acl.builder-functions-test
  (:require
    [clojure.test :refer :all]
    [framework.components.acl.builder-functions :refer [add-actions
                                                        override-actions
                                                        remove-resource
                                                        allow
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


(-> (add-actions {} {"comment" :read
                     "post"    [:read :delete :update :comment]})
    (add-actions {"comment" :delete}))

(-> (add-actions {} {"comment" :read
                     "post"    [:read :delete :update :comment]})
    (override-actions {"comment" :delete}))

(-> (add-actions {} {"comment" :read
                     "post"    [:read :delete :update :comment]})
    (add-actions {"comment" :delete})
    (remove-resource "post"))
