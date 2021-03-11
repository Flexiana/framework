(ns framework.components.acl.builder-functions)

(defn collify
  [x]
  (if (coll? x) x [x]))

(defn add-actions
  [available-permissions action-map]
  (reduce (fn [perms [resource actions]]
            (update perms resource concat (collify actions)))
    available-permissions action-map))

(defn override-actions
  [available-permissions action-map]
  (merge available-permissions action-map))

(defn remove-resource
  [available-permissions resource]
  (dissoc available-permissions resource))

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
        actions-vec (collify actions)
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

(defn deny
  [available-permissions permissions {:keys [role resource actions restriction] :or {restriction :all} :as permission}]
  (let [actions-vec (collify actions)
        permissions-by-resource (->> (get permissions role)
                                     (filter #(#{resource :all} (:resource %))))
        available-by-resource (get available-permissions resource)]))
