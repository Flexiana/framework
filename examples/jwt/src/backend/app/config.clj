(ns app.config)

;; This def is for readability only
;; the xiana/jwt config should be passed
;; as an env variable so the private and public
;; keys don't need to be tracked by version control.

(def private-key (slurp "resources/_files/jwtRS256.key"))

(def public-key (slurp "resources/_files/jwtRS256.key.pub"))

(defn init-jwt-cfg
  [config]
  (let [jwt-config (:xiana/jwt config)]
    (-> jwt-config
        (assoc-in [:auth :public-key] public-key)
        (assoc-in [:auth :private-key] private-key)
        (->>
          (assoc config :xiana/jwt)))))
