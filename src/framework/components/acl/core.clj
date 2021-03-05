(ns framework.components.acl.core)

(defn isAllowed
  [{permissions :users/permissions} {:keys [role resource privilege]}]
  (let [owning (->> (get permissions role)
                    (filter #(#{resource :all} (:resource %)))
                    first)]
    (if (some #{privilege :all} (:actions owning))
      (:filter owning)
      false)))

