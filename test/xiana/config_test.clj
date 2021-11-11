(ns xiana.config-test
  (:require
    [clojure.test :refer :all]
    [xiana.config :as config]))

(def config-map (config/config))

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
                (:xiana/web-server config-map)
                (:xiana/postgresql config-map)
                (:xiana/migration config-map)
                (:xiana/emails config-map)
                (:xiana/auth config-map))
              nil))))

;; test if the web-server map is not empty
(deftest web-server-map-not-empty
  (let [web-server-map (:xiana/web-server config-map)]
    (is (= (empty? web-server-map) false))))

;; test if the web-server map contains the expected keys (:port/:join?)
(deftest contain-expected-web-server-map-keys
  (let [web-server-map (:xiana/web-server config-map)]
    (is (not (= (and (map? web-server-map)
                     (:port web-server-map)
                     (:join? web-server-map))
                nil)))))
