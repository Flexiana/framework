(ns xiana.hash-test
  (:require
    [clojure.test :refer [deftest is]]
    [xiana.hash :as hash]))

(def password "myPersonalPassword!")

(defn testing-ok
  [settings]
  (let [encrypted (hash/make settings password)]
    (is (true? (hash/check settings password encrypted)))))

(defn testing-mistake
  [settings]
  (let [encrypted (hash/make settings password)]
    (is (false? (hash/check settings "myWrongPassword!" encrypted)))))

(deftest test-full-functionality-bcrypt
  (let [fragment {:deps {:auth {:hash-algorithm :bcrypt}}}]
    (testing-mistake fragment)
    (testing-ok fragment)))

(deftest test-full-functionality-script
  (let [fragment {:deps {:auth {:hash-algorithm :scrypt}}}]
    (testing-mistake fragment)
    (testing-ok fragment)))

(deftest test-full-functionality-pbkdf2
  (let [fragment {:deps {:auth {:hash-algorithm :pbkdf2}}}]
    (testing-mistake fragment)
    (testing-ok fragment)))

(deftest test-assert-functionality
  (let [fragment {:deps {:auth {:hash-algorithm :argon2}}}]
    (is (thrown? java.lang.AssertionError (hash/make fragment password)))))

(deftest hash-behavior
  (let [pwd "not_nil"
        state {:deps {:auth {:hash-algorithm  :bcrypt
                             :bcrypt-settings {:work-factor 11}}}}
        hash1 (hash/make state pwd)
        hash2 (hash/make state pwd)]
    (is (false? (hash/check state hash1 hash2)))
    (is (not= hash1 hash2))
    (is (true? (and (hash/check state pwd hash1)
                    (hash/check state pwd hash2))))))
