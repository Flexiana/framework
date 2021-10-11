(ns todoapp.models.todos
  (:require
    [clojure.core :as core]
    [honeysql.helpers :refer [select
                              from
                              where]
     :as helpers]
    [xiana.core :as xiana])
  (:import
    (java.util
      UUID)))

(defn ->store-user
  [m]
  (->> (select-keys m [:id
                       :title
                       :is_done])
       (into {})))

(defn list-query
  [state]
  (xiana/ok (assoc state :query (cond-> (-> (select :*)
                                            (from :todos))))))
