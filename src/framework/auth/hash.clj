(ns framework.auth.hash
  (:require [crypto.password.bcrypt :as hash-b]
            [crypto.password.pbkdf2 :as hash-p]
            [crypto.password.scrypt :as hash-s]))

(defn- dispatch
  ([_state _password]
   (dispatch _state _password nil))
  ([{settings :framework.app/auth} _password _encrypted]
   (let [key (:hash-algorithm settings)
         supported [:bcrypt :pbkdf2 :scrypt]
         algorithm? (contains? supported key)]
     (if algorithm? key :bcrypt))))

(defmulti make dispatch)

(defmethod make :bcrypt [_state password]
  (hash-b/encrypt password))

(defmethod make :scrypt [_state password]
  (hash-s/encrypt password))

(defmethod make :pbkdf2 [_state password]
  (hash-p/encrypt password))

(defmulti check dispatch)

(defmethod check :bcrypt [_state password encrypted]
  (hash-b/check password encrypted))

(defmethod check :scrypt [_state password encrypted]
  (hash-s/check password encrypted))

(defmethod check :pbkdf2 [_state password encrypted]
  (hash-p/check password encrypted))
