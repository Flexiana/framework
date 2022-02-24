(ns framework.session.session-component-test
  (:require
    [clojure.test :refer :all]
    [framework-fixture :as fixture]
    [framework.session.core :as session]
    [next.jdbc.result-set]))

(def system-config
  {:xiana/session-backend {:storage            :database
                           :session-table-name :sessions}
   :xiana/jdbc-opts       {:builder-fn next.jdbc.result-set/as-kebab-maps}})

(use-fixtures :once (partial fixture/std-system-fixture system-config))

(def session-state
  {#uuid"6552a891-4139-48b5-af53-ac13d02d3ba5" {:session-id "6552a891-4139-48b5-af53-ac13d02d3ba5",
                                                :title      "form"
                                                :index      0}
   #uuid"0558b901-ca0b-4c2e-8e47-6fa96e666f66" {:session-id "0558b901-ca0b-4c2e-8e47-6fa96e666f66",
                                                :title      "meat"
                                                :index      1}
   #uuid"5d1bd69c-d712-4cd1-b62f-57ecacad99e7" {:session-id "5d1bd69c-d712-4cd1-b62f-57ecacad99e7",
                                                :index      2,
                                                :title      "arabica"}})

(deftest testing-session-dump-where
  (let [sb (:session-backend @fixture/test-system)]
    (session/erase! sb)
    (doseq [[session-id session-data] session-state]
      (session/add! sb session-id session-data))
    (testing "Sessions prepared"
      (is (= (vals session-state)
             (->> (session/dump sb)
                  vals
                  (map #(dissoc % :modified-at))))))
    (testing "equality"
      (is (= (val (second session-state))
             (-> (session/dump-where sb [:= :session-id "0558b901-ca0b-4c2e-8e47-6fa96e666f66"])
                 vals
                 first
                 (dissoc :modified-at)))))
    (testing "not equal"
      (is (= 2 (count (session/dump-where sb [:not [:= :session-id "0558b901-ca0b-4c2e-8e47-6fa96e666f66"]])))))
    (testing "select by title"
      (is (= (val (last session-state))
             (-> (session/dump-where sb [:= :title "arabica"])
                 vals
                 first
                 (dissoc :modified-at)))))
    (testing "select via IN"
      (is (= (val (last session-state))
             (-> (session/dump-where sb [:in :index [2 3]])
                 vals
                 first
                 (dissoc :modified-at)))))
    (testing "select via BETWEEN"
      (is (= 2 (count (session/dump-where sb [:between :index 1 3])))))
    (testing "and with result"
      (is (= 1 (count (session/dump-where sb [:and
                                              [:in :title ["form" "meat" "loaf" "arabica"]]
                                              [:= :index 2]])))))
    (testing "and without result"
      (is (= 0 (count (session/dump-where sb [:and
                                              [:in :title ["form" "meat" "loaf" "arabica"]]
                                              [:= :index 4]])))))
    (testing "or function"
      (is (= 3 (count (session/dump-where sb [:or
                                              [:in :title ["form" "meat" "loaf"]]
                                              [:= :index 2]])))))))

