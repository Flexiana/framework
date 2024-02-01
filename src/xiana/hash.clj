(ns xiana.hash
  "Cryptography helper for creating, and resolving passwords.
  Supported algorithms are bcrypr, pbkdf2, and scrypt.
  The required algorithm should be in (-> state :deps :auth :hash-algorithm)"
  (:require
    [crypto.password.bcrypt :as hash-b]
    [crypto.password.pbkdf2 :as hash-p]
    [crypto.password.scrypt :as hash-s]
    [crypto.password.argon2 :as argon2]))

(def supported [:bcrypt :pbkdf2 :scrypt :argon2])

(defn- dispatch
  ([state password]
   (dispatch state password nil))
  ([{{{hash-algorithm :hash-algorithm} :auth} :deps} _ _]
   {:pre [(some #(= hash-algorithm %) supported)]}
   hash-algorithm))

(defmulti make
  "Creating an encrypted version for store password."
  dispatch)

(defmethod make :bcrypt
  [{{:keys [bcrypt-settings]
     :or   {bcrypt-settings {:work-factor 11}}} :deps/auth}
   password]
  (hash-b/encrypt password (:work-factor bcrypt-settings)))

(defmethod make :scrypt
  [{{:keys [scrypt-settings]
     :or   {scrypt-settings {:cpu-cost        32768
                             :memory-cost     8
                             :parallelization 1}}} :deps/auth}
   password]
  (hash-s/encrypt
    password
    (:cpu-cost scrypt-settings)
    (:memory-cost scrypt-settings)
    (:parallelization scrypt-settings)))

(defmethod make :pbkdf2
  [{{:keys [pbkdf2-settings]
     :or   {pbkdf2-settings {:type       :sha1
                             :iterations 100000}}} :deps/auth}
   password]
  (hash-p/encrypt
    password
    (:iterations pbkdf2-settings)
    (if (= :sha1 (:type pbkdf2-settings))
      "HMAC-SHA1" "HMAC-SHA256")))

(defmulti check
  "Validating password."
  dispatch)

(defmethod check :bcrypt [_ password encrypted]
  (hash-b/check password encrypted))

(defmethod check :scrypt [_ password encrypted]
  (hash-s/check password encrypted))

(defmethod check :pbkdf2 [_ password encrypted]
  (hash-p/check password encrypted))
