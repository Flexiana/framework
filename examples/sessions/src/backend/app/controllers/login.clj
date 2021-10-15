(ns controllers.login
  (:require
    [clojure.data.json :as json]
    [ring.util.request :refer [body-string]]
    [xiana.core :as xiana])
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

(defn login-view
  [{request :request :as state}]
  (let [rbody (some-> request
                      body-string
                      (json/read-str :key-fn keyword))
        user (find-user (-> rbody
                            :email))
        session-id (UUID/randomUUID)
        session-data {:session-id session-id
                      :user       (dissoc user :password)}]
    (if (and user (= (:password user) (:password rbody)))
      (xiana/ok (assoc state
                       :session-data session-data
                       :response {:status  200
                                  :headers {"Content-Type" "application/json"}
                                  :body    (json/write-str (update session-data :session-id str))}))

      (xiana/error (assoc state :response {:status 401
                                           :body   "Incorrect credentials"})))))

(defn login-controller
  [state]
  (xiana/flow-> state
                login-view))

