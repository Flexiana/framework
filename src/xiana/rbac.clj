(ns xiana.rbac
  "Integrates tiny-RBAC library to the xiana flow"
  (:require
    [tiny-rbac.builder :as b]
    [tiny-rbac.core :as c]))

(defn init
  "Initialize and validates a role-set"
  [config]
  (assoc config :role-set (b/init (:xiana/role-set config))))

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
  if the given user has no rights, it throws ex-info with data {:status 403 :body \"Forbidden\"}.
  On leave executes restriction function if any."
  {:enter (fn [state]
            (let [operation-restricted (get-in state [:request-data :permission])
                  permits              (and operation-restricted (permissions state))]
              (cond
                (and operation-restricted (empty? permits)) (throw (ex-info "Forbidden" {:status 403 :body "Forbidden"}))
                operation-restricted                        (assoc-in state [:request-data :user-permissions] permits)
                :else                                       state)))
   :leave (fn [state]
            (let [restriction-fn (get-in state [:request-data :restriction-fn] identity)]
              (restriction-fn state)))})
