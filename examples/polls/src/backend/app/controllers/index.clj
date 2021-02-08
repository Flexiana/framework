(ns controllers.index
  (:require [xiana.core :as xiana]))

(defn index-view
  [state]
  (xiana/ok
    (assoc state
      :response
      {:status  200
       :headers {"Content-Type" "text/plain"}
       :body    "Index page"})))

(defn require-logged-in [{req :http-request :as state}]
  (if-let [authorization (get-in req [:headers "authorization"])]
    (xiana/ok (assoc-in state [:session-data :authorization] authorization))
    (xiana/error (assoc state :response {:status 401 :body "Unauthorized"}))))

(defn something-else
  [state]
  (xiana/ok state))

;(defn comment-action []
;  (controller->
;    (validator/process-form)
;    (model/save-to-database)
;    (session/set-flash-message "Comment was saved")
;    (response/redirect :HERE)
;
;    :invalid-comment (controller->
;                       (response/populate-form)
;                       (view/set :error "Comment is invalid"))
;    ))

(defn index
  [state]
  (xiana/flow->
    state
    (require-logged-in)
    (something-else)
    (index-view)))
