(ns xiana.jwt
  (:require
    [buddy.core.keys :as keys]
    [buddy.sign.jwt :as jwt]
    [buddy.sign.util :as util]))

(defn- calculate-time-claims
  "Calculates exp and nbf posix values according to the configuration
  and merge them into out-claims map"
  [out-claims]
  (let [exp-cfg (get out-claims :exp 0)
        nbf-cfg (get out-claims :nbf 0)]
    (merge out-claims {:exp (+ (util/now) exp-cfg)
                       :nbf (+ (util/now) nbf-cfg)
                       :iat (util/now)})))

(defmulti verify-jwt
  "Checks if the signature of a token is correct given the algorithm and public-key
  provided in the configuration. Returns the payload in case of success
  and an ExceptionInfo in case it fails."
  (fn [type _ _]
    type))

(defmethod verify-jwt :claims
  [_ token {:keys [alg public-key in-claims]}]
  (let [pkey (keys/str->public-key public-key)
        claims (when (seq in-claims)
                 (assoc in-claims :now (util/now)))]
    (try
      (jwt/unsign token pkey (merge {:alg alg} claims))
      (catch NullPointerException _
        (throw (ex-info "Failed to unsign JWT!"
                        {:cause :wrong-key}))))))

(defmethod verify-jwt :no-claims
  [_ token {:keys [alg public-key]}]
  (let [pkey (keys/str->public-key public-key)]
    (try
      (jwt/unsign token pkey {:alg alg})
      (catch NullPointerException _
        (throw (ex-info "Failed to unsign JWT!"
                        {:cause :wrong-key}))))))

(defmulti sign
  "Tries to sign a token given the algorithm and private-key
  provided in the configuration. Returns the signed token."
  (fn
    [type _ _]
    type))

(defmethod sign :claims
  [_ payload {:keys [alg private-key out-claims]}]
  (let [pkey (keys/str->private-key private-key)
        claims (-> payload
                   (merge out-claims)
                   (calculate-time-claims))]
    (jwt/sign claims pkey {:alg alg})))

(defmethod sign :no-claims
  [_ payload {:keys [alg private-key]}]
  (let [pkey (keys/str->private-key private-key)]
    (jwt/sign payload pkey {:alg alg})))
