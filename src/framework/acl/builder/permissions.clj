(ns framework.acl.builder.permissions
  (:require
    [framework.acl.builder.builder-functions :as b]
    [xiana.core :as xiana]))

(defn init
  ([{ap :acl/available-permissions :as state}]
   (if ap
     (init state ap)
     (init state {})))
  ([state available-permissions]
   (let [ap (into {} (b/collify-vals available-permissions))]
     (xiana/ok (assoc state :acl/available-permissions ap)))))

(defn add-actions
  [{ap :acl/available-permissions :as state} action-map]
  (xiana/ok (assoc state :acl/available-permissions (b/add-actions ap action-map))))

(defn override-actions
  [{ap :acl/available-permissions :as state} action-map]
  (xiana/ok (assoc state :acl/available-permissions (b/override-actions ap action-map))))

(defn remove-resource
  [{ap :acl/available-permissions :as state} action-map]
  (xiana/ok (assoc state :acl/available-permissions (b/remove-resource ap action-map))))

