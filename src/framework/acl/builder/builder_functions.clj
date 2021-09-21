(ns framework.acl.builder.builder-functions)


(defn collify
  [x]
  (if (and (not (map? x)) (coll? x)) x [x]))


(defn collify-vals
  [m]
  (->> (map (fn [[k v]] [k (collify v)]) m)
       (into {})))


(defn distinct-concat
  [x y]
  (distinct (into x y)))


(defn add-actions
  [available-permissions action-map]
  (reduce (fn [perms [resource actions]]
            (update perms resource distinct-concat (collify actions)))
          available-permissions action-map))


(defn override-actions
  [available-permissions action-map]
  (merge available-permissions (collify-vals action-map)))


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
                      (distinct (concat actions (collify action))))]
    (assoc permission :actions new-actions)))


(defn ->permission
  [{a :actions r :over :as p}]
  (cond-> (select-keys p [:resource :actions :over])
    (not (coll? a)) (assoc :actions [a])
    (not r) (assoc :over :all)))


(defn allow
  "Allows a permission for a role, inserts it into the :acl/roles map.
  If (:actions permission) or (:actions :roles) keys contains :all it's going to be [:all]"
  ([roles available-permissions permission]
   (let [new-roles (allow roles permission)]
     (into {} (map (fn [[k vs]]
                     [k (for [v vs
                              :let [ap (get available-permissions (:resource v))]]
                          (if (every? (into #{} (:actions v)) ap)
                            (assoc v :actions [:all])
                            v))])
                   new-roles))))
  ([roles {:keys [role resource actions over] :or {over :all} :as permission}]
   (let [new-permission (->permission permission)
         actions-vec (collify actions)
         permissions-by-resource (->> (get roles role)
                                      (filter #(#{resource :all} (:resource %))))
         new-roles (reduce (fn [permissions action]
                             (let [same-action (->> permissions
                                                    (filter #(some (hash-set action :all) (:actions %)))
                                                    first)
                                   same-restricted (->> permissions
                                                        (filter #(#{over} (:over %)))
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
       (assoc roles role [(assoc new-permission :actions [:all])])
       (assoc roles role new-roles)))))


(defn bulk-revoke
  [permissions-by-resource actions-vec]
  (reduce (fn [perms action]
            (for [perm perms] (revoke perm action)))
          permissions-by-resource actions-vec))


(defn deny
  "Denies an access for a user/group on a resource
  If a role contains :actions [:all]
  and no available permissions provided for that resource,
  it will delete the role"
  [roles available-permissions {:keys [role resource actions]}]
  (let [actions-vec (collify actions)
        permissions-by-role (get roles role)
        permissions-by-resource (if (= resource :all)
                                  permissions-by-role
                                  (->> permissions-by-role
                                       (filter #((hash-set resource :all) (:resource %)))))
        has-all-access (into #{} (filter #(#{[:all]} (:actions %)) permissions-by-resource))
        other-permissions (->> permissions-by-role
                               (remove (into #{} permissions-by-resource)))
        available-by-resource (get available-permissions resource)]
    (->> (cond
           (some #{:all} actions-vec) (assoc roles role other-permissions)
           (not-empty has-all-access) (assoc roles role (concat
                                                          other-permissions
                                                          (bulk-revoke (remove has-all-access permissions-by-resource) actions-vec)
                                                          (map #(grant
                                                                  (assoc % :actions [])
                                                                  (remove (into #{} actions-vec) available-by-resource))
                                                               has-all-access)))
           permissions-by-resource (assoc roles role (concat other-permissions (bulk-revoke permissions-by-resource actions-vec)))
           :else roles)
         (remove nil?)
         (map (fn [[k v]] [k (remove #(or (nil? %) (empty? (:actions %))) v)]))
         (remove #(empty? (second %)))
         (into {}))))
