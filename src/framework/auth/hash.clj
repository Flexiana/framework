(ns framework.auth.hash
  (:require
    [crypto.password.bcrypt :as hash-b]
    [crypto.password.pbkdf2 :as hash-p]
    [crypto.password.scrypt :as hash-s]))

(def supported [:bcrypt :pbkdf2 :scrypt])

(defn- dispatch
  ([state password]
   (dispatch state password nil))
  ([{{:keys [hash-algorithm]} :framework.app/auth} _ _]
   {:pre [(some #(= hash-algorithm %) supported)]}
   hash-algorithm))

(defmulti make dispatch)

(defmethod make :bcrypt
  [{{:keys [bcrypt-settings]
     :or {bcrypt-settings {:work-factor 11}}} :framework.app/auth}
   password]
  (hash-b/encrypt password (:work-factor bcrypt-settings)))

(defmethod make :scrypt
  [{{:keys [scrypt-settings]
     :or {scrypt-settings {:cpu-cost 32768
                           :memory-cost 8
                           :parallelization 1}}} :framework.app/auth}
   password]
  (hash-s/encrypt
    password
    (:cpu-cost scrypt-settings)
    (:memory-cost scrypt-settings)
    (:parallelization scrypt-settings)))

(defmethod make :pbkdf2
  [{{:keys [pbkdf2-settings]
     :or {pbkdf2-settings {:type :sha1
                           :iterations 100000}}} :framework.app/auth}
   password]
  (hash-p/encrypt
    password
    (:iterations pbkdf2-settings)
    (if (= :sha1 (:type pbkdf2-settings))
      "HMAC-SHA1" "HMAC-SHA256")))

(defmulti check dispatch)

(defmethod check :bcrypt [_ password encrypted]
  (hash-b/check password encrypted))

(defmethod check :scrypt [_ password encrypted]
  (hash-s/check password encrypted))

(defmethod check :pbkdf2 [_ password encrypted]
  (hash-p/check password encrypted))
