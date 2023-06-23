(ns xiana.jwt.action
  (:require [xiana.jwt :as jwt]))

(defn refresh-token
  [state]
  (let [jwt-authentication (get-in state [:session-data :jwt-authentication])
        cfg (get-in state [:deps :xiana/jwt :auth])
        jwt-token (jwt/sign :claims jwt-authentication cfg)]
    (assoc state :response {:status 200 :body {:auth-token jwt-token}})))
