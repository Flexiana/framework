(ns xiana.jwt.interceptors
  (:require
    [clojure.string :as cstr]
    [xiana.jwt :as jwt]
    [xiana.route.helpers :as helpers])
  (:import
    (clojure.lang
      ExceptionInfo)))

(def jwt-auth
  {:name ::jwt-authentication
   :enter
   (fn [{request :request :as state}]
     (let [auth (-> request
                    (get-in [:headers :authorization])
                    (cstr/split #" ")
                    second)
           cfg (get-in state [:deps :xiana/jwt :auth])]
       (try
         (->>
           (jwt/verify-jwt :claims auth cfg)
           (assoc state :session-data))
         (catch ExceptionInfo e
           (assoc state :error e)))))
   :error
   (fn [state]
     (let [error (:error state)
           err-info (ex-data error)]
       (cond
         (= :exp (:cause err-info))
         (helpers/unauthorized state "JWT Token expired.")
         (= :validation (:type err-info))
         (helpers/unauthorized state "One or more Claims were invalid.")
         :else
         (helpers/unauthorized state "Signature could not be verified."))))})

(def jwt-content
  {:name ::jwt-content-exchange
   :enter
   (fn [{request :request :as state}]
     (if-let [body-params (:body-params request)]
       (let [cfg (get-in state [:deps :xiana/jwt :content])]
         (try
           (->> (jwt/verify-jwt :no-claims body-params cfg)
                (assoc-in state [:request :body-params]))
           (catch ExceptionInfo e
             (assoc state :error e))))
       state))
   :leave
   (fn [{response :response :as state}]
     (let [cfg (get-in state [:deps :xiana/jwt :content])]
       (->> (jwt/sign :no-claims (:body response) cfg)
            (assoc-in state [:state :response :body]))))
   :error
   (fn [state]
     (helpers/unauthorized state "Signature could not be verified"))})
