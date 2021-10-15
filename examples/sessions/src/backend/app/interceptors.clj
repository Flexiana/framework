(ns app.interceptors
  (:require
    [framework.session.core :refer [delete! fetch]]
    [xiana.core :as x])
  (:import
    (java.util
      UUID)))

(def require-logged-in
  {:enter
   (fn [state]
     (if (get-in state [:session-data :user])
       (x/ok state)
       (x/error (assoc state :response {:status 401 :body "Unauthorized"}))))})

(def logout
  {:leave (fn [state]
            (let [sessions-backend (-> state
                                       :deps
                                       :session-backend)
                  session-id (-> state
                                 :session-data
                                 :session-id)]
              (delete! sessions-backend session-id)
              (x/ok (dissoc state :session-data))))})

(def inject-session?
  {:enter (fn [{{headers      :headers
                 cookies      :cookies
                 query-params :query-params} :request
                :as                          state}]
            (try (let [session-backend (-> state :deps :session-backend)
                       session-id (UUID/fromString (or (some->> headers
                                                                :session-id)
                                                       (some->> cookies
                                                                :session-id
                                                                :value)
                                                       (some->> query-params
                                                                :SESSIONID)))
                       session-data (fetch session-backend session-id)]
                   (x/ok (assoc state :session-data (assoc session-data :session-id session-id))))
                 (catch Exception _
                   (x/ok state))))})
