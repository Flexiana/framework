(ns app.controllers.login
  (:require
    [clojure.data.json :as json]
    [ring.util.request :refer [body-string]]
    [xiana.core :as x])
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
  (x/error (assoc state :response {:status 401
                                   :body   "Missing credentials"})))

(defn login-view
  [{request :request :as state}]
  (try (let [rbody (or (some-> request
                               body-string
                               (json/read-str :key-fn keyword))
                       (throw (ex-message "Missing body")))
             user (find-user (-> rbody
                                 :email))
             session-id (UUID/randomUUID)
             session-data {:session-id session-id
                           :user       (dissoc user :password)}]
         (if (and user (= (:password user) (:password rbody)))
           (x/ok (assoc state
                        :session-data session-data
                        :response {:status  200
                                   :headers {"Content-Type" "application/json"}
                                   :body    (json/write-str (update session-data :session-id str))}))

           (x/error (assoc state :response {:status 401
                                            :body   "Incorrect credentials"}))))
       (catch Exception _ (missing-credentials state))))

(defn login-controller
  [state]
  (x/flow-> state
            login-view))

