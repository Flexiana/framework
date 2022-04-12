(ns cli-chat.views.common)

(defn response
  [state body]
  (->
    state
    (assoc-in [:response :status] 200)
    (assoc-in [:response :headers "Content-type"] "Application/json")
    (assoc-in [:response :body] body)))

(defn not-allowed
  [_]
  (throw (ex-info "You don't have rights to do this"
                  {:status 401 :body "You don't have rights to do this"})))
