(ns app.controllers.login
  (:require
    [xiana.jwt :as jwt]
    [xiana.route.helpers :as helpers]))

(def db
  [{:id         1
    :email      "xiana@test.com"
    :first-name "Xiana"
    :last-name  "Developer"
    :password   "topsecret"}])

(defn find-user
  [email]
  (first (filter (fn [i]
                   (= email (:email i))) db)))

(defn missing-credentials
  [state]
  (assoc state :response {:status 401
                          :body   "Missing credentials"}))

(defn login-controller
  [{request :request :as state}]
  (try (let [rbody (or (:body-params request)
                       (throw (ex-message "Missing body")))
             user (find-user (:email rbody))]
         (if (and user (= (:password user) (:password rbody)))
           (let [cfg (get-in state [:deps :xiana/jwt :auth])
                 jwt-token (jwt/sign :claims (dissoc user :password) cfg)]
             (assoc state :response {:status 200 :body {:auth-token jwt-token}}))
           (helpers/unauthorized state "Incorrect credentials")))
       (catch Exception e
         (println e)
         (missing-credentials state))))
