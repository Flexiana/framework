(ns framework.config.core-test
  (:require
   [clojure.test :refer :all]
   [framework.config.core :as config]))

(def config-map (config/read-edn-file nil))

;; test if the configuration map is not empty
(deftest config-map-not-empty
  (is (= (empty? config-map) false)))

;; test if contains an arbitrary key that should not be present
;; if equal to nil -> true, otherwise false
(deftest wrong-key-not-present
  (is (= (:non-existent-key config-map) nil)))

;; test if the default keys are present
(deftest default-keys-are-present
  (is (not (= (and
               (:framework.app/web-server config-map)
               (:framework.db.storage/postgresql config-map)
               (:framework.db.storage/migration config-map)
               (:framework.app/emails config-map)
               (:framework.app/auth config-map))
              nil))))

;; test if the web-server map is not empty
(deftest web-server-map-not-empty
  (let [web-server-map (:framework.app/web-server config-map)]
    (is (= (empty? web-server-map) false))))

;; test if the web-server map contains the expected keys (:port/:join?)
(deftest contain-expected-web-server-map-keys
  (let [web-server-map (:framework.app/web-server config-map)]
    (is (not (= (and (map? web-server-map)
                     (:port web-server-map)
                     (:join? web-server-map))
                nil)))))
