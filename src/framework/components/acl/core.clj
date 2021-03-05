(ns framework.components.acl.core)

(defn isAllowed
  [{permissions :users/permissions} [{:keys [role resource privilege] :as request}]]
  (if-let [filter (some->> (get  permissions role)
                           (filter #{resource})
                           first
                           :actions
                           (some #{privilege}))]
    filter
    false))
