(ns framework.rbac.core
  "Integrates tiny-RBAC library to the xiana flow"
  (:require
    [tiny-rbac.builder :as b]
    [tiny-rbac.core :as c]
    [xiana.core :as xiana]))

(defn init
  "Initialize and validates a role-set"
  [role-set]
  (b/init role-set))

(defn permissions
  "Gathers the necessary parameters from xiana state for permission resolution.
  Returns a set of keywords for data ownership check.
  The format of returned keywords:
   ':resource/restriction'"
  [state]
  (let [role-set (get-in state [:deps :role-set])
        role (get-in state [:session-data :users/role])
        permit (get-in state [:request-data :permission])
        resource (keyword (namespace permit))
        action (keyword (name permit))
        permissions (c/permissions role-set role resource action)]
    (into #{} (map #(keyword (str (name resource) "/" (name %))) permissions))))

(def interceptor
  "On enter it validates if the resource is restricted,
  and available at the current state (actual user with a role)
  If it's not restricted do nothing,
  if the given user has no rights, responses {:status 403 :body \"Forbidden\"}.
  On leave executes restriction function if any."
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
