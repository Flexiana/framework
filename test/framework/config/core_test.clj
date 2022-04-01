(ns framework.config.core-test
  (:require
    [clojure.test :refer :all]
    [framework.config.core :as config]))

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
  (is (every? some?
              [(get-in config-map [:xiana/web-server])
               (get-in config-map [:xiana/postgresql])
               (get-in config-map [:xiana/migration])
               (get-in config-map [:xiana/emails])
               (get-in config-map [:xiana/auth])])))

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

(deftest override-with-map
  (let [config (config/config {:xiana/web-server {:port 5000}
                               :xiana/postgresql {:user "Nobody"}
                               :xiana/emails {:from "xiana@"}})]
    (is (= 5000 (get-in config [:xiana/web-server :port])))
    (is (= {:join? false :port  5000}
           (get-in config [:xiana/web-server])))
    (is (= {:dbname     "framework"
            :dbtype     "postgresql"
            :host       "localhost"
            :image-name "postgres:14-alpine"
            :password   "postgres"
            :port       5432
            :user       "Nobody"}
           (get-in config [:xiana/postgresql])))
    (is (= {:host "", :user "", :pass "", :tls true, :port 587, :from "xiana@"}
           (get-in config [:xiana/emails])))))

(deftest read-value-from-env
  ;; pls break it via executing:
  ;; env SOMETHING_ELSE='else' lein test
  ;; env PROPERTY='something' lein test

  (is (= {:test-value-1 nil, :test-value-2 "default"}
         (:xiana/test (config/config))))
  (is (= {:test-value-1 "test-property", :test-value-2 "default"}
         (:xiana/test (config/config {:property "test-property"}))))
  (is (= {:test-value-1 nil, :test-value-2 "test-property"}
         (:xiana/test (config/config {:something-else "test-property"})))))
