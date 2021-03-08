(ns framework.components.session.core-test
  (:require
    [clojure.test :refer [deftest
                          is]]
    [framework.components.session.backend :refer [init-in-memory-session
                                                  add!
                                                  fetch
                                                  delete!]]))

(deftest test-in-memory-session-store
  (let [user-id (java.util.UUID/randomUUID)
        session (init-in-memory-session)]
    (add! session :user-id {:id user-id})
    (is (=  {:id user-id} (fetch session :user-id)))
    (delete! session :user-id)
    (is (nil? (fetch session :user-id)))))
