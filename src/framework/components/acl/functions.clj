(ns framework.components.acl.functions)

(defn grantee
  [permissions {:keys [role resource privilege]}]
  (->> (get permissions role)
       (filter #(#{resource :all} (:resource %)))
       (filter #(some #{privilege :all} (:actions %)))
       first))

(defn has-access
  "Examine if the user is has access to a resource with the provided action.
  If it has, returns anything what is provided in ':acl/roles' corresponding :restriction field.
  If isn't then returns \"false\"
  ':acl/roles' is a map keyed by name of :acl/roles.
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

(defn replace-role
  [roles old new]
  (conj (remove #{old} roles) new))

(defn revoke
  [{actions :actions :as permission} action]
  {:pre [(not-empty permission)]}
  (let [new-actions (remove #{action} actions)]
    (when-not (empty? new-actions) (assoc permission :actions new-actions))))

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
  "Allows a permission for a role, inserts it into the :acl/roles map.
  If (:actions permission) or (:actions :permissions) keys contains :all it's going to be [:all]"
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

(defn init
  ([state]
   (if (:acl/available-permissions state)
     state
     (assoc state :acl/available-permissions {})))
  ([state available-permissions]
   (assoc state :acl/available-permissions available-permissions)))

(defn add-actions
  [available-permissions action-map]
  (reduce (fn [perms [resource actions]]
            (update perms resource concat (if (coll? actions) actions [actions])))
    available-permissions action-map))

(defn deny
  [available-permissions permissions {:keys [role resource actions restriction] :or {restriction :all} :as permission}]
  (let [actions-vec (if (coll? actions) actions [actions])
        permissions-by-resource (->> (get permissions role)
                                     (filter #(#{resource :all} (:resource %))))
        available-by-resource (get available-permissions resource)]))
