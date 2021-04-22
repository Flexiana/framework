(ns framework.one-enpoint-functions
  (:require
    [clojure.test :refer :all]
    [honeysql.helpers :refer [select
                              from
                              where]]
    [xiana.core :as xiana])
  (:import
    (java.util
      UUID)))

(defn get-user-view
  [{response-data :response-data :as state}]
  (println "View")
  (xiana/ok (->
              state
              (assoc-in [:response :status] 200)
              (assoc-in [:response :headers "Content-type"] "Application/json")
              (assoc-in [:response :body] {:view-type "users"
                                           :data      {:users (->> response-data
                                                                   :db-data)}}))))

(defn get-user-query
  [{{{id :id} :query-params} :request
    :as                      state}]
  (println "Action!")
  (xiana/ok (assoc state :query (cond-> (-> (select :*)
                                            (from :users))
                                  id (where [:= :id (UUID/fromString id)])))))

(def views-map
  {[:users :get] get-user-view})

(def action-map
  {[:users :get] get-user-query})

(defn get-user-controller
  [state]
  (xiana/flow->
    (assoc state :view get-user-view)
    get-user-query))
