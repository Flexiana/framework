(ns framework.components.acl.permissions-builder
  (:require
    [framework.components.acl.builder-functions :as b]
    [xiana.core :as xiana]))

(defn init
  ([state]
   (xiana/ok (if (:acl/available-permissions state)
               state
               (init state {}))))
  ([state available-permissions]
   (xiana/ok (assoc state :acl/available-permissions available-permissions))))

(defn add-actions
  [{ap :acl/available-permissions :as state} action-map]
  (xiana/ok (assoc state :acl/available-permissions (b/add-actions ap action-map))))

(defn override-actions
  [{ap :acl/available-permissions :as state} action-map]
  (xiana/ok (assoc state :acl/available-permissions (b/override-actions ap action-map))))

(defn remove-resource
  [{ap :acl/available-permissions :as state} action-map]
  (xiana/ok (assoc state :acl/available-permissions (b/remove-resource ap action-map))))

