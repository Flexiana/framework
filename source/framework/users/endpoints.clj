(ns framework.users.endpoints
  (:require
    [framework.users.actions :as actions]))


(def endpoints
  ["/"
   ["users" {:get actions/all
             :post actions/create}]
   ["users/supply" {:get actions/supply}]
   ["users/greet" {:get actions/greet}]])
