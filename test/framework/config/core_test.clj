(ns framework.config.core-test
  (:require
   [clojure.test :refer :all]
   [framework.config.core :as config]))

(def config-map (config/read-edn-file nil))

(deftest its-key-present
  (is (:framework.db.storage/postgresql config-map)))

(deftest its-key-not-present
  (is (= (:non-existent-key config-map) nil)))
