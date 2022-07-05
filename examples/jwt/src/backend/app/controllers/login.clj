(ns app.controllers.login
  (:require
    [clojure.data.json :as json]
    [ring.util.request :refer [body-string]]
    [xiana.route.helpers :as helpers]
    [xiana.jwt :as jwt])
  (:import
    (java.util
      UUID)))

(def db
  [{:id         1
    :email      "piotr@example.com"
    :first-name "Piotr"
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
             user (find-user (-> rbody :email))]
         (if (and user (= (:password user) (:password rbody)))
           (let [cfg (get-in state [:deps :xiana/jwt :auth])
                 jwt-token (jwt/sign :auth (dissoc rbody :password) cfg)]
             (assoc state :response {:status 200 :body {:auth-token jwt-token}}))
           (helpers/unauthorized "Incorrect credentials")))
       (catch Exception _ (missing-credentials state))))
