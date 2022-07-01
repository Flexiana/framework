(ns xiana.jwt
  (:require
   [buddy.core.keys :as keys]
   [buddy.sign.jwt :as jwt]
   [buddy.sign.util :as util]
   [clojure.edn :as edn]))

(defn- safe-str->pkey
  [str-pkey]
  (try
    (keys/str->public-key str-pkey)
    (catch org.bouncycastle.openssl.PEMException e
      (throw (IllegalArgumentException.
               (str "The public key provided is not a valid public key."))))))

(defn- safe-str->privkey
  [str-pkey]
  (try
    (keys/str->private-key str-pkey)
    (catch org.bouncycastle.openssl.PEMException e
      (throw (IllegalArgumentException.
              (str "The private key provided is not a valid private key."))))))

(defn- calculate-exp-claim
  "Adds the exp arg (in seconds) to the current time (UTC)."
  [exp]
  (+ (util/now)
     exp))


(defmulti unsign
  "Tries to unsign a token given the algorithm and public-key
  provided in the configuration. Returns the payload in case of success
  and an ExceptionInfo in case it fails."
  (fn [type _ _]
    type))

(defmethod unsign :auth
  [_ token {:keys [alg public-key in-claims] :as cfg}]
  (let [pkey (safe-str->pkey public-key)
        claims (when (seq in-claims)
                 (assoc in-claims :now (util/now)))]
    (try
      (jwt/unsign token pkey (merge {:alg alg} claims))
      (catch NullPointerException e
        (throw (ex-info "Failed to unsign JWT!"
                        {:cause "wrong public key."}))))))

(defmethod unsign :content
  [_ token {:keys [alg public-key] :as cfg}]
  (let [pkey (safe-str->pkey public-key)]
    (try
      (jwt/unsign token pkey {:alg alg})
      (catch NullPointerException e
        (throw (ex-info "Failed to unsign JWT!"
                        {:cause "wrong public key."}))))))

(defmulti sign
  "Tries to sign a token given the algorithm and private-key
  provided in the configuration. Returns the payload in case of success
  and an ExceptionInfo in case it fails."
  (fn 
    [type _ _]
    type))

(defmethod sign :auth
  [_ payload {:keys [alg private-key out-claims] :as cfg}]
  (let [pkey (safe-str->privkey private-key)
        exp (calculate-exp-claim (:exp out-claims))
        claims (-> payload
                   (merge out-claims)
                   (assoc :exp exp))] 
    (jwt/sign claims pkey {:alg alg})))

(defmethod sign :content
  [_ payload {:keys [alg private-key] :as cfg}]
  (let [pkey (safe-str->privkey private-key)]
    (jwt/sign payload pkey {:alg alg})))
