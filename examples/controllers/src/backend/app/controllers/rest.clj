(ns controllers.rest
  (:require
    [jsonista.core :as json]
    [xiana.core :as xiana]))


(defn coerce
  [state]
  (let [content-type (-> state :http-request :headers (get "accept"))
        {{:keys [body status]} :response} state
        formatter (case content-type
                    "application/json" json/write-value-as-string
                    "application/xml" #(str "<tag>" (name %1) "</tag>"))]
    (xiana/ok
      (assoc state
        :response
        {:status  status
         :headers {"Content-Type" content-type}
         :body    (formatter body)}))))

(defn require-logged-in
  [{req :http-request :as state}]
  (if-let [authorization (get-in req [:headers "authorization"])]
    (xiana/ok (assoc-in state [:session-data :authorization] authorization))
    (xiana/error (assoc state :response {:status 401 :body "Unauthorized"}))))

(defn rest-view
  [{req :http-request {handler :handler} :request-data :as state}]
  (try
    ;TODO add extra field to xiana/State to hold raw response, response before the Content-Type is applied
    (xiana/ok (assoc state :response (handler req)))
    (catch Exception e
      ;TODO I would like to see something logged here
      (xiana/error (assoc state :response {:status 500 :body "Internal Server error"})))))

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

(defn ctrl
  [state]
  (xiana/flow->
    state
    (require-logged-in)
    (rest-view)
    (coerce)))
