(ns cli-chat.models.users)

(defn get-user
  [user-name]
  {:select [:*]
   :from   [:users]
   :where  [:= :name user-name]})
