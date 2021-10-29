(ns framework.acl.builder
  "This package will be removed in the next version, use rbac instead"
  {:deprecated true}
  (:require
    [framework.acl.builder.permissions :as abp]
    [framework.acl.builder.roles :as abr]
    [xiana.core :as xiana]))

(defn init
  [this config]
  (xiana/flow->
    this
    (abp/init (:acl/permissions config))
    (abr/init (:acl/roles config))))
