(ns framework.components.acl.core)

(defn isAllowed
  "Examine if the user is able to access to a resource with the provided action.
  If it's able to, returns anything what is provided in 'permissions' corresponding :filter field.
  If it's isn't then returns \"false\"
  'permissions' is a map keyed by name of permissions.
  'user' is optional, but if it missing you must provide the 'role' field in action.
  'access' defines the role, resource and privilege what needs to be achieved.
  If user is provided, the role will be resolved from it."
  ([permissions user access]
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

