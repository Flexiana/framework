(ns framework.security.authentication.http
  (:require [buddy.auth :as buddy-auth]
            [buddy.auth.backends :as buddy-auth-backends]
            [buddy.auth.backends.httpbasic :as buddy-auth-backends-httpbasic]
            [buddy.auth.middleware :as buddy-auth-middleware]
            [buddy.hashers :as buddy-hashers]
            [framework.security.authentication.jwt :refer [create-token]]))


(defn basic-auth
  "Authentication function called from basic-auth middleware for each
  request. The result of this function will be added to the request
  under key :identity.
  NOTE: Use HTTP Basic authentication always with HTTPS in real setups."
  [db request {:keys [username password]}]
  (let [user (get db username)]
    (if (and user (buddy-hashers/check password (:password user)))
      (-> user
          (dissoc :password)
          (assoc :token (create-token user)))
      false)))

(defn create-basic-auth-backend
  "Creates basic-auth backend to be used by basic-auth-middleware."
  [db]
  (buddy-auth-backends-httpbasic/http-basic-backend
   {:authfn (partial basic-auth db)}))

(defn create-basic-auth-middleware
  "Creates a middleware that authenticates requests using http-basic
  authentication."
  [db]
  (let [backend (create-basic-auth-backend db)]
    (fn [handler]
      (buddy-auth-middleware/wrap-authentication handler backend))))
