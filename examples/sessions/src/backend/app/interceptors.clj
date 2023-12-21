(ns app.interceptors
  (:require
    [xiana.session :refer [delete! fetch]]))

(def require-logged-in
  {:name ::require-logged-in
   :enter
   (fn [state]
     (if (get-in state [:session-data :user])
       state
       (throw (ex-info "Unauthorized" {:xiana/response {:status 401 :body "Unauthorized"}}))))})

(def logout
  {:name ::logout
   :leave (fn [state]
            (let [sessions-backend (-> state
                                       :deps
                                       :session-backend)
                  session-id       (-> state
                                       :session-data
                                       :session-id)]
              (delete! sessions-backend session-id)
              (dissoc state :session-data)))})

(def inject-session?
  {:name ::inject-session?
   :enter (fn [{{headers      :headers
                 cookies      :cookies
                 query-params :query-params} :request
                :as                          state}]
            (try (let [session-backend (-> state :deps :session-backend)
                       session-id      (parse-uuid (or (some->> headers
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
