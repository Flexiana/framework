(ns framework.security.passwords
  (:require [buddy.hashers :as hashers]))


(defn encrypt
  "encrpyts a password, set type of encryption in options"
  ([password]
  (hashers/derive password))
  ([password options]
   (hashers/derive password options)))


(defn check
  [password encrypted-password]
  (hashers/check password encrypted-password))