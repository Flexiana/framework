(ns interceptors.load-user
  (:require
    [framework.db.sql :as db]
    [honeysql.helpers :refer [select from where]]
    [xiana.core :as xiana])
  (:import (java.util UUID)))

(def login-user
  {:enter (fn [{:keys [request] :as state}]
            (try (if-let [user-id (UUID/fromString (get-in request [:headers :authorization]))]
                   (let [datasource (get-in state [:deps :db :datasource])
                         query (-> {}
                                   (select :*)
                                   (from :users)
                                   (where [:= :id user-id]))
                         user (first (db/execute datasource query))]
                     (xiana/ok (assoc-in state [:session-data :user] user))))
                 (catch Exception _
                   (xiana/ok (assoc-in state [:session-data :user] {:users/role :guest
                                                                    :users/id (UUID/randomUUID)})))))})
