(ns controllers.posts
  (:require
    [honeysql.helpers :refer :all :as helpers]
    [views.posts]
    [xiana.core :as xiana])
  (:import
    (java.util
      UUID)))

(defn fetch-posts
  [{{{id :id} :query-params} :request
    :as                      state}]
  (if id
    (xiana/flow->
      state
      views.posts/post-view)
    (xiana/flow->
      state
      views.posts/all-posts)))

(defn update-post
  [state]
  (xiana/flow->
    state
    views.posts/all-posts))

(defn create-post
  [state]
  (xiana/flow->
    state
    views.posts/all-posts))

(defn delete-post
  [state]
  (xiana/flow->
    state
    views.posts/all-posts))

(def view-map
  {:get    fetch-posts
   :post   update-post
   :put    create-post
   :delete delete-post})

(defn select-view
  "Inserts view, depends on request-method"
  [{{method :request-method} :request :as state} view-map]
  (xiana/ok (assoc state :view (view-map method))))

(def query-map
  "Basic SQL queries by request-method"
  {:get    (-> (select :*)
               (from :posts))
   :post   (helpers/update :posts)
   :put    (insert-into :posts)
   :delete (delete-from :posts)})

(defn base-query
  "Inserting SQL query into state"
  [{{method :request-method} :request :as state} query-map]
  (xiana/ok (assoc state :query (query-map method))))

(defn handle-id-map
  [query id method]
  (when id (let [uuid-id (UUID/fromString id)]
             (get {:get    (-> query (where [:= :id uuid-id]))
                   :post   (-> query (where [:= :id uuid-id]))
                   :delete (-> query (where [:= :id uuid-id]))}
                  method))))

(defn handle-id
  "Add where clause to SQL query if it's necessary"
  [{{{id :id} :query-params
     method   :request-method} :request
    query                      :query
    :as                        state}
   handle-id-map]
  (if-let [new-query (handle-id-map query id method)]
    (xiana/ok (assoc state :query new-query))
    (xiana/ok state)))

(defn handle-body-map
  [query form-params user-id method]
  (let [content (:content form-params)]
    (get {:post (-> query (sset {:content content}))
          :put  (-> query (columns :content :user_id) (values [[content user-id]]))}
         method)))

(defn handle-body
  "Add content from form params"
  [{{form-params :form-params
     method      :request-method}                    :request
    {{{{user-id :id} :user} :session-data} :session} :deps
    query                                            :query
    :as                                              state}
   handle-body-map]
  (if-let [new-query (handle-body-map query form-params user-id method)]
    (xiana/ok (assoc state :query new-query))
    (xiana/ok state)))

(defn controller
  "Example controller for ACL, and DataOwnership"
  [state]
  (xiana/flow->
    state
    (select-view view-map)
    (base-query query-map)
    (handle-id handle-id-map)
    (handle-body handle-body-map)))
