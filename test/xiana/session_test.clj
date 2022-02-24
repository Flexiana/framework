(ns xiana.session-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [xiana.session :as session])
  (:import
    (java.util
      UUID)))

;; initial session-instance instance
(def session-instance (:session-backend (session/init-backend {})))

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

(deftest dump-where
  (session/erase! session-instance)
  (let [titles ["marketing" "salmon" "form" "meat" "impulse"]
        session-ids (repeatedly 5 #(UUID/randomUUID))
        f-session (first session-ids)
        sessions (map (fn [id idx title] {:session-id id
                                          :index idx
                                          :title title})
                      session-ids (range) titles)]
    (doseq [session sessions]
      (session/add! session-instance (:session-id session) session))
    (is (= 5 (count (session/dump session-instance))))
    (testing "equal to"
      (is {f-session {:session-id f-session :index 0 :title "marketing"}}
          (session/dump-where session-instance [:= :session-id f-session])))
    (testing "not equal to"
      (is (= 4 (count (session/dump-where session-instance [:not [:= :session-id f-session]])))))
    (testing "!="
      (is (= 4 (count (session/dump-where session-instance [:!= :session-id f-session])))))
    (testing "<>"
      (is (= 4 (count (session/dump-where session-instance [:<> :session-id f-session])))))
    (testing ">"
      (is (= 1 (count (session/dump-where session-instance [:> :index 3])))))
    (testing "<"
      (is (= 3 (count (session/dump-where session-instance [:< :index 3])))))
    (testing "<="
      (is (= 4 (count (session/dump-where session-instance [:<= :index 3])))))
    (testing ">="
      (is (= 2 (count (session/dump-where session-instance [:>= :index 3])))))
    (testing "between"
      (is (= 2 (count (session/dump-where session-instance [:between :index 3 4])))))
    (testing "not between"
      (is (= 3 (count (session/dump-where session-instance [:not [:between :index 3 4]])))))
    (testing "like sub"
      (is (= 1 (count (session/dump-where session-instance [:like :title "%orm"])))))
    (testing "like full"
      (is (= 1 (count (session/dump-where session-instance [:like :title "marketing"])))))
    (testing "like multiple"
      (is (= 2 (count (session/dump-where session-instance [:like :title "m%"])))))
    (testing "like all"
      (is (= 5 (count (session/dump-where session-instance [:like :title "%m%"])))))
    (testing "like some"
      (is (= 2 (count (session/dump-where session-instance [:like :title "%m%n%"])))))
    (testing "not like some"
      (is (= 3 (count (session/dump-where session-instance [:not [:like :title "%m%n%"]])))))
    (testing "in"
      (is (= 2 (count (session/dump-where session-instance [:in :title ["form" "meat" "loaf"]])))))
    (testing "not in"
      (is (= 3 (count (session/dump-where session-instance [:not [:in :title ["form" "meat" "loaf"]]])))))
    (testing "and in some"
      (is (= 1 (count (session/dump-where session-instance [:and
                                                            [:in :title ["form" "meat" "loaf"]]
                                                            [:= :index 2]])))))
    (testing "or in some"
      (is (= 3 (count (session/dump-where session-instance [:or
                                                            [:in :title ["form" "meat" "loaf"]]
                                                            [:= :index 4]])))))))

