(ns framework.components.acl.core
  (:require
    [xiana.core :as xiana]))

(defn grantee
  [permissions {:keys [role resource privilege]}]
  (->> (get permissions role)
       (filter #(#{resource :all} (:resource %)))
       (filter #(some #{privilege :all} (:actions %)))
       first))

(defn has-access
  "Examine if the user is has access to a resource with the provided action.
  If it has, returns anything what is provided in 'permissions' corresponding :restriction field.
  If isn't then returns \"false\"
  'permissions' is a map keyed by name of permissions.
  'user' is optional, but if it missing you must provide the 'role' field in action.
  'access' defines the role, resource and privilege what needs to be achieved.
  If user is provided, the role will be resolved from it."
  ([permissions user access]
   (cond
     (:role user) (has-access permissions (assoc access :role (:role user)))
     (not (:is_active user)) (has-access permissions (assoc access :role :guest))
     (:is_superuser user) (has-access permissions (assoc access :role :superuser))
     (:is_staff user) (has-access permissions (assoc access :role :staff))
     (:is_active user) (has-access permissions (assoc access :role :member))))
  ([permissions access]
   (if-let [granted (grantee permissions access)]
     (:restriction granted)
     false)))

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
  ([{{user :user} :session permissions :acl/permissions :as state} access]
   (let [result (if (:role user)
                  (has-access permissions (assoc access :role (:role user)))
                  (has-access permissions user access))]
     (cond result (xiana/ok (assoc-in state [:response-data :acl] result))
           (:or-else access) ((:or-else access) state)
           :else (xiana/error (assoc state :response {:status 401 :body "Authorization error"})))))
  ([{{user :user} :session http-request :http-request :as state}]
   (let [permissions (:acl/permissions state)
         resource (->resource (:uri http-request))
         privilege (action-mapping (:request-method http-request))
         result (if (:role user)
                  (has-access permissions {:resource resource :privilege privilege :role (:role user)})
                  (has-access permissions user {:resource resource :privilege privilege}))]
     (if result
       (xiana/ok (assoc-in state [:response-data :acl] result))
       (xiana/error (assoc state :response {:status 401 :body "Authorization error"}))))))

(defn replace-role
  [roles old new]
  (conj (remove #{old} roles) new))

(defn revoke
  [{actions :actions :as permission} action]
  {:pre [(not-empty permission)]}
  (let [new-actions (remove #{action} actions)]
    (if-not (empty? new-actions) (assoc permission :actions new-actions))))

(defn grant
  [{actions :actions :as permission} action]
  {:pre [(not-empty permission)]}
  (let [new-actions (if (or (some #{:all} actions) (= :all action))
                      [:all]
                      (distinct (conj actions action)))]
    (assoc permission :actions new-actions)))

(defn ->permission
  [{a :actions r :restriction :as p}]
  (cond-> (select-keys p [:resource :actions :restriction])
    (not (coll? a)) (assoc :actions [a])
    (not r) (assoc :restriction :all)))

(defn allow
  [permissions {:keys [role resource actions restriction] :or {restriction :all} :as permission}]
  (let [new-permission (->permission permission)
        actions-vec (if (coll? actions) actions [actions])
        permissions-by-resource (->> (get permissions role)
                                     (filter #(#{resource :all} (:resource %))))
        new-permissions (reduce (fn [permissions action]
                                  (let [same-action (->> permissions
                                                         (filter #(some (hash-set action :all) (:actions %)))
                                                         first)
                                        same-restricted (->> permissions
                                                             (filter #(#{restriction} (:restriction %)))
                                                             first)]
                                    (cond
                                      (and same-action same-restricted) permissions
                                      (and same-action (= [:all] (:actions same-action))) permissions
                                      same-action (remove nil? (-> (replace-role permissions same-action (revoke same-action action))
                                                                   (conj new-permission)))
                                      same-restricted (->> (grant same-restricted action)
                                                           (replace-role permissions same-restricted))
                                      :else (conj permissions new-permission))))
                          permissions-by-resource actions-vec)]
    (if (some #{:all} actions-vec)
      (assoc permissions role [(assoc new-permission :actions [:all])])
      (assoc permissions role new-permissions))))

;allow and deny
