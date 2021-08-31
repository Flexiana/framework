(ns conway.users.endpoints
  (:require
    [conway.users.actions :as actions]))


(def endpoints
  ["/"
   ["users" {:get actions/all
             :post actions/create}]
   ["users/supply" {:get actions/supply}]
   ["users/greet" {:get actions/greet}]])
