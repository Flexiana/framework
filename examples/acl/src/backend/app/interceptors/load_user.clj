(ns interceptors.load-user
  (:require
    [framework.db.sql :as db]
    [honeysql.helpers :refer [select from where]]
    [xiana.core :as xiana]
    [framework.session.core :as session])
  (:import (java.util UUID)))

(defn ->role
  [user]
  (let [role (cond
               (not (:users/is_active user)) :guest
               (:users/is_superuser user) :superuser
               (:users/is_staff user) :staff
               (:users/is_active user) :member
               :else :guest)]
    (assoc user :users/role role)))

(def login-user
  {:enter (fn [{:keys [request] :as state}]
            (try (if-let [user-id (UUID/fromString (get-in request [:headers :authorization]))]
                   (let [datasource (get-in state [:deps :db :datasource])
                         query (-> {}
                                   (select :*)
                                   (from :users)
                                   (where [:= :id user-id]))
                         user (-> (db/execute datasource query)
                                  first
                                  ->role)
                         session-backend (-> state :deps :session-backend)
                         session-id (get-in request [:headers :session-id] (UUID/randomUUID))]
                     (session/add! session-backend session-id user)
                     (xiana/ok (-> (assoc-in state [:session-data :user] user)
                                   (assoc-in [:request :headers "session-id"] session-id)))))
                 (catch Exception _
                   (xiana/ok (assoc-in state [:session-data :user] {:users/role :guest
                                                                    :users/id   (UUID/randomUUID)})))))})
