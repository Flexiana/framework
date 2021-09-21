(ns framework.acl.builder
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
