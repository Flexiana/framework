(ns framework.components.acl.core)

(defn isAllowed
  ([config user access]
   (cond
     (not (:is_active user)) (isAllowed config (assoc access :role :guest))
     (:is_superuser user) (isAllowed config (assoc access :role :superuser))
     (:is_staff user) (isAllowed config (assoc access :role :staff))
     (:is_active user) (isAllowed config (assoc access :role :member))))
  ([{permissions :users/permissions} {:keys [role resource privilege]}]
   (let [granted (->> (get permissions role)
                      (filter #(#{resource :all} (:resource %)))
                      first)]
     (if (some #{privilege :all} (:actions granted))
       (:filter granted)
       false))))

