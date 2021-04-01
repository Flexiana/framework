(ns empty-controller
  (:require
    [xiana.core :as xiana]))

(defn controller
  "Example controller for ACL, and DataOwnership"
  [state]
  (println "in controller " state)
  (xiana/ok state))
