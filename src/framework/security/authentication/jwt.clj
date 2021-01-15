(ns framework.security.authentication.jwt
  (:require
   [buddy.auth :as buddy-auth]
   [buddy.auth.backends :as buddy-auth-backends]
   [buddy.auth.middleware :as buddy-auth-middleware]
   [buddy.hashers :as buddy-hashers]
   [buddy.sign.jwt :as jwt]
   [framework.config.core :as config]))

(def private-key
  (:private-key config))

(defn create-token
  "Creates a signed jwt-token with user data as payload.
  `valid-seconds` sets the expiration span."
  [user & {:keys [valid-seconds] :or {valid-seconds 7200}}] ;; 2 hours
  (let [payload (-> user
                    (select-keys [:id :roles])
                    (assoc :exp (.plusSeconds
                                 (java.time.Instant/now) valid-seconds)))]
    (jwt/sign payload private-key {:alg :hs512})))

(def token-backend
  "Backend for verifying JWT-tokens."
  (buddy-auth-backends/jws {:secret private-key :options {:alg :hs512}}))

(defn token-auth-middleware
  "Middleware used on routes requiring token authentication."
  [handler]
  (buddy-auth-middleware/wrap-authentication handler token-backend))