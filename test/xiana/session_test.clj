(ns xiana.session-test
  (:require
    [clojure.test :refer [deftest is]]
    [xiana.session :as session]))

;; initial session-instance instance
(def session-instance (:session-backend (session/init-backend {})))

;; test add and fetch reify implementations
(deftest session-protocol-add!-fetch
  (let [user-id (random-uuid)]
    ;; add user id
    (session/add! session-instance :user-id {:id user-id})
    ;; verify if user ids are equal
    (is (= {:id user-id}
           (session/fetch session-instance :user-id)))))

;; test delete! reify implementation
(deftest session-protocol-delete!
  (let [user-id (session/fetch session-instance :user-id)]
    ;; remove user identification
    (when user-id
      (session/delete! session-instance :user-id))
    ;; verify if was removed
    (is (nil? (session/fetch session-instance :user-id)))))

;; test erase reify implementation
(deftest session-protocol-add!-erase!
  (let [user-id (random-uuid)
        session-id (random-uuid)]
    ;; add session instance values
    (session/add! session-instance :user-id {:id user-id})
    (session/add! session-instance :session-id {:id session-id})
    ;; verify if the values exists
    (is (= {:id user-id} (session/fetch session-instance :user-id)))
    (is (= {:id session-id} (session/fetch session-instance :session-id)))
    ;; erase and verify if session instance is empty
    (is (empty? (session/erase! session-instance)))))
