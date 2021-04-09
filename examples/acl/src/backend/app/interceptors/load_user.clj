(ns interceptors.load-user
  (:require
    [clojure.core :as core]
    [framework.db.sql :as db]
    [honeysql.core :as sql]
    [honeysql.helpers :as helpers]
    [xiana.core :as xiana]))

(defn- purify
  "Removes namespaces from keywords"
  [elem]
  (into {}
        (map (fn [[k v]] {(keyword (name k)) v}) elem)))

(defn load-user
  [{{{user-id :id} :user} :session-data
    :as                   state}]
  (let [query (-> (helpers/select :*)
                  (helpers/from :users)
                  (helpers/where [:= :id user-id])
                  sql/format)
        user (first (db/execute state query))]
    (if user
      (xiana/ok (-> (assoc-in state [:session-data :user] (purify user))
                    (core/update :session-data dissoc :new-session)))
      (xiana/ok state))))
