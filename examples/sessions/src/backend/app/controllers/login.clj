(ns controllers.login
  (:require
    [clojure.data.json :as json]
    [ring.util.request :refer [body-string]]
    [xiana.core :as xiana]))

(def db
  [{:id "piotr@example.com"
    :first-name "Piotr"
    :last-name "Developer"
    :password "topsecret"}])

(defn find-user
  [email]
  (first  (filter (fn [i]
                    (= email (:id i))) db)))

(defn login-view
  [{request :http-request :as state}]
  (let [rbody (-> request
                  body-string
                  (json/read-str :key-fn keyword))
        user (find-user (-> rbody
                            :email))
        session-id (str (java.util.UUID/randomUUID))]
    (if (= (:password user) (:password rbody))
      (xiana/ok (assoc state
                       :login-data (merge  {:session-id (str session-id)}
                                           (dissoc user :password))
                       :response
                       {:status  200
                        :headers {"Content-Type" "application/json"}
                        :body   (json/write-str
                                  (merge (dissoc user :password)
                                         {:session-id session-id}))}))

      (xiana/error (assoc state :response {:status 401
                                           :body "Incorrect credentials"})))))

(defn login-controller
  [state]

  (xiana/flow-> state
                login-view))

