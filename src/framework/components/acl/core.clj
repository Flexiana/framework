(ns framework.components.acl.core)

(defn isAllowed
  ([config user access]
   (cond
     (not (:is_active user)) (isAllowed permissions (assoc access :role :guest))
     (:is_superuser user) (isAllowed permissions (assoc access :role :superuser))
     (:is_staff user) (isAllowed permissions (assoc access :role :staff))
     (:is_active user) (isAllowed permissions (assoc access :role :member))))
  ([permissions {:keys [role resource privilege]}]
   (let [granted (->> (get permissions role)
                      (filter #(#{resource :all} (:resource %)))
                      first)]
     (if (some #{privilege :all} (:actions granted))
       (:filter granted)
       false))))

