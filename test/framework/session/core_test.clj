(ns framework.session.core-test
  (:require
   [clojure.test :refer :all]
   [framework.session.core :as session])
  (:import
   (java.util UUID)))

;; initial session-instance instance
(def session-instance (session/init-in-memory))

;; test add and fetch reify implementations
(deftest session-protocol-add!-fetch
  (let [user-id (UUID/randomUUID)]
    ;; add user id
    (session/add! session-instance :user-id {:id user-id})
    ;; verify if user ids are equal
    (is (= {:id user-id}
           (session/fetch session-instance :user-id)))))

;; test delete! reify implementation
(deftest session-protocol-delete!
  (let [user-id (session/fetch session-instance :user-id)]
    ;; remove user identification
    (session/delete! session-instance :user-id)
    ;; verify if was removed
    (is (nil? (session/fetch session-instance :user-id)))))

;; test erase reify implementation
(deftest session-protocol-add!-erase!
  (let [user-id (UUID/randomUUID)
        session-id (UUID/randomUUID)]
    ;; add session instance values
    (session/add! session-instance :user-id {:id user-id})
    (session/add! session-instance :session-id {:id session-id})
    ;; verify if the values exists
    (is (= {:id user-id} (session/fetch session-instance :user-id)))
    (is (= {:id session-id} (session/fetch session-instance :session-id)))
    ;; erase and verify if session instance is empty
    (is (empty? (session/erase! session-instance)))))
