(ns app.controllers.login
  (:require
    [clojure.data.json :as json]
    [ring.util.request :refer [body-string]]
    [xiana.session :as session])
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
  (try (let [rbody (or (some-> request
                               body-string
                               (json/read-str :key-fn keyword))
                       (throw (ex-message "Missing body")))
             user (find-user (:email rbody))
             session-id (UUID/randomUUID)
             session-data {:session-id session-id
                           :user       (dissoc user :password)}]
         (if (and user (= (:password user) (:password rbody)))
           (let [session-backend (get-in state [:deps :session-backend])]
             (session/add! session-backend session-id session-data)
             (assoc state
                    :response {:status  200
                               :headers {"Content-Type" "application/json"
                                         "Session-id"   (str session-id)}
                               :body    (json/write-str (update session-data :session-id str))}))
           (assoc state :response {:status 401
                                   :body   "Incorrect credentials"})))
       (catch Exception _ (missing-credentials state))))
