(ns interceptors.load-user
  (:require
    [honeysql.helpers :refer [select from where]]
    [xiana.db :as db]))

(defn ->role
  [user]
  (let [role (cond
               (not (:users/is_active user)) :guest
               (:users/is_superuser user) :superuser
               (:users/is_staff user) :staff
               (:users/is_active user) :member
               :else :guest)]
    (assoc user :users/role role)))

(defn valid-user?
  [{:keys [request] :as state}]
  (let [user-id (parse-uuid (get-in request [:headers :authorization]))
        datasource (get-in state [:deps :db :datasource])
        query (-> {}
                  (select :*)
                  (from :users)
                  (where [:= :id user-id]))]
    (-> (db/execute datasource query)
        first
        ->role)))

(def load-user!
  {:enter (fn [{:keys [request] :as state}]
            (let [guest-user {:users/role :guest
                              :users/id   (random-uuid)}
                  session-id (get-in request [:headers :session-id] (random-uuid))
                  user (try (let [valid-user (valid-user? state)]
                              (if (empty? valid-user)
                                guest-user
                                valid-user))
                            (catch Exception _ guest-user))]
              (assoc state :session-data  (assoc user :session-id session-id))))})
