(ns framework.rbac.core
  (:require
    [framework.session.core :as session]
    [tiny-rbac.builder :as b]
    [tiny-rbac.core :as c]
    [xiana.core :as xiana]))

(defn init
  [role-set]
  {:role-set (b/init role-set)})

(defn permissions
  [state]
  (let [role-set (get-in state [:deps :role-set])
        session-id (get-in state [:request :headers "Session-id"])
        session-backend (-> state :deps :session-backend)
        user (session/fetch session-backend session-id)
        role (:users/role user)
        permit (get-in state [:request-data :match :permission])
        resource (keyword (namespace permit))
        action (keyword (name permit))
        permissions (c/permissions role-set role resource action)]
    (into #{} (map #(keyword (str (name resource) "/" (name %))) permissions))))

(defn restrict
  [{query :query
    :as state}]
  (if query (let [restriction-fns (get-in state [:request-data :restriction-fns])
                  user-permissions (get-in state [:request-data :user-permissions])]
              (if (and user-permissions restriction-fns)
                (reduce (fn [s p]
                          ((restriction-fns p) s)) state user-permissions)
                state))
            state))

(def interceptor
  {:enter (fn [state]
            (let [operation-restricted (get-in state [:request-data :match :permission])
                  permits (and operation-restricted (permissions state))]
              (cond
                (and operation-restricted (empty? permits)) (xiana/error {:response {:status 403 :body "Forbidden"}})
                operation-restricted (xiana/ok (assoc-in state [:request-data :user-permissions] permits))
                :else (xiana/ok state))))
   :leave (fn [state]
            (xiana/ok (restrict state)))})
