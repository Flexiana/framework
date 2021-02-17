(ns framework.auth.hash-test
  (:require
    [clojure.test :refer [deftest is]]
    [framework.auth.hash :as hash]))

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
  (let [fragment {:framework.app/auth {:hash-algorithm :bcrypt}}]
    (testing-mistake fragment)
    (testing-ok fragment)))

(deftest test-full-functionality-script
  (let [fragment {:framework.app/auth {:hash-algorithm :scrypt}}]
    (testing-mistake fragment)
    (testing-ok fragment)))

(deftest test-full-functionality-pbkdf2
  (let [fragment {:framework.app/auth {:hash-algorithm :pbkdf2}}]
    (testing-mistake fragment)
    (testing-ok fragment)))

(deftest test-assert-functionality
  (let [fragment {:framework.app/auth {:hash-algorithm :argon2}}]
    (is (thrown? java.lang.AssertionError (hash/make fragment password)))))
