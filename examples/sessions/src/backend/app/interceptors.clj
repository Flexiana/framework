(ns app.interceptors
  (:require
    [xiana.session :refer [delete! fetch]])
  (:import
    (java.util
      UUID)))

(def require-logged-in
  {:name ::require-logged-in
   :enter
   (fn [state]
     (if (get-in state [:session-data :user])
       state
       (throw (ex-info "Unauthorized" {:status 401 :body "Unauthorized"}))))})

(def inject-session?
  {:name ::inject-session?
   :enter (fn [{{headers      :headers
                 cookies      :cookies
                 query-params :query-params} :request
                :as                          state}]
            (try (let [session-backend (-> state :deps :session-backend)
                       session-id      (UUID/fromString (or (some->> headers
                                                                     :session-id)
                                                            (some->> cookies
                                                                     :session-id
                                                                     :value)
                                                            (some->> query-params
                                                                     :SESSIONID)))
                       session-data    (fetch session-backend session-id)]
                   (assoc state :session-data (assoc session-data :session-id session-id)))
                 (catch Exception _  ; TODO: catch more specific exception or rethink that
                   state)))})
