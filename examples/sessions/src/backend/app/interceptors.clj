(ns app.interceptors
  (:require
    [xiana.core :as xiana]
    [xiana.session :refer [delete! fetch]])
  (:import
    (java.util
      UUID)))

(def require-logged-in
  {:enter
   (fn [state]
     (if (get-in state [:session-data :user])
       (xiana/ok state)
       (xiana/error (assoc state :response {:status 401 :body "Unauthorized"}))))})

(def logout
  {:leave (fn [state]
            (let [sessions-backend (-> state
                                       :deps
                                       :session-backend)
                  session-id (-> state
                                 :session-data
                                 :session-id)]
              (delete! sessions-backend session-id)
              (xiana/ok (dissoc state :session-data))))})

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
                   (xiana/ok (assoc state :session-data (assoc session-data :session-id session-id))))
                 (catch Exception _
                   (xiana/ok state))))})
