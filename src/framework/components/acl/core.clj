(ns framework.components.acl.core
  (:require
    [framework.components.acl.functions :refer [has-access]]
    [xiana.core :as xiana]))

(def action-mapping
  {:get    :read
   :post   :update
   :put    :create
   :delete :delete})

(defn ->resource
  [uri]
  (re-find #"\w+" uri))

(defn is-allowed
  "Checks if the user is able to do an action on a resource.
  Returns xiana/ok when it is, and extends [:response-data :acl] with the restriction of ownership check.
  When the user has no access, returns xiana/error or executes ((:or-else access) state) if it's provided.
  If no 'access' has been provided, it's resolves:
  - resource from URI (/users/ -> \"users\")
  - and privilege from request method:

  |req:    | action: |
  |------- |---------|
  |:get    | :read   |
  |:post   | :update |
  |:put    | :create |
  |:delete | :delete |"
  ([{{user :user} :session permissions :acl/roles :as state} access]
   (let [result (if (:role user)
                  (has-access permissions (assoc access :role (:role user)))
                  (has-access permissions user access))]
     (cond result (xiana/ok (assoc-in state [:response-data :acl] result))
           (:or-else access) ((:or-else access) state)
           :else (xiana/error (assoc state :response {:status 401 :body "Authorization error"})))))
  ([{{user :user} :session http-request :http-request :as state}]
   (let [permissions (:acl/roles state)
         resource (->resource (:uri http-request))
         privilege (action-mapping (:request-method http-request))
         result (if (:role user)
                  (has-access permissions {:resource resource :privilege privilege :role (:role user)})
                  (has-access permissions user {:resource resource :privilege privilege}))]
     (if result
       (xiana/ok (assoc-in state [:response-data :acl] result))
       (xiana/error (assoc state :response {:status 401 :body "Authorization error"}))))))

