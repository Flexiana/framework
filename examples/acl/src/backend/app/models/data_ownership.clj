(ns models.data-ownership
  (:require
    [honeysql.helpers :refer [merge-where]]))

(defn owner-fn
  [state]
  (let [user-permissions (get-in state [:request-data :user-permissions])
        query (:query state)
        user-id (get-in state [:session-data :users/id])]
    (assoc state :query
           (cond
             (user-permissions :comments/own) (merge-where query [:= :comments.user_id user-id])
             (user-permissions :users/own) (merge-where query [:= :users.id user-id])
             (user-permissions :posts/own) (merge-where query [:= :posts.user_id user-id])
             :else query))))
