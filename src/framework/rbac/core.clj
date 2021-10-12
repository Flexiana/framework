(ns framework.rbac.core
  (:require
    [framework.session.core :as session]
    [tiny-rbac.builder :as b]
    [tiny-rbac.core :as c]
    [xiana.core :as xiana]))

(defn init
  [role-set]
  (b/init role-set))

(defn permissions
  [state]
  (let [role-set (get-in state [:deps :role-set])
        user (get-in state [:session-data :user])
        role (:users/role user)
        permit (get-in state [:request-data :permission])
        resource (keyword (namespace permit))
        action (keyword (name permit))
        permissions (c/permissions role-set role resource action)]
    (into #{} (map #(keyword (str (name resource) "/" (name %))) permissions))))

(def interceptor
  {:enter (fn [state]
            (let [operation-restricted (get-in state [:request-data :permission])
                  permits (and operation-restricted (permissions state))]
              (cond
                (and operation-restricted (empty? permits)) (xiana/error {:response {:status 403 :body "Forbidden"}})
                operation-restricted (xiana/ok (assoc-in state [:request-data :user-permissions] permits))
                :else (xiana/ok state))))
   :leave (fn [state]
            (if-let [restriction-fn (get-in state [:request-data :restriction-fn])]
              (restriction-fn state)
              (xiana/ok state)))})
