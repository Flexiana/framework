(ns interceptors)

(def require-logged-in
  {:enter (fn [{request :request :as state}]
            (if-let [authorization (get-in request [:headers :authorization])]
              (assoc-in state [:session-data :authorization] authorization)
              (throw (ex-info "Unauthorized" {:status 401 :body "Unauthorized"}))))})
