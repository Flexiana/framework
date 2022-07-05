(ns app.config)


(def private-key (slurp "resources/_files/jwtRS256.key"))

(def public-key (slurp "resources/_files/jwtRS256.key.pub"))


;;This def is for readability only
;;the xiana/jwt config should be passed
;;as an env variable so the private and public
;;keys don't need to be tracked by version control.
(def jwt-config
  {:xiana/jwt
   {:auth
    {:public-key public-key
     :private-key private-key
     :alg :rs256
     :in-claims {:iss "xiana-api"
                 :aud "api-consumer"
                 :leeway 0
                 :max-age 40}
     :out-claims {:exp 1000
                  :iss "xiana-api"
                  :aud "api-consumer"
                  :nbf 0}}}})
