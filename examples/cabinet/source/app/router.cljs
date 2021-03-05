(ns app.router
  (:require
    [app.welcome.views.login :as login]
    [reagent.core :as r]
    [reitit.coercion.spec :as rcs]
    [reitit.frontend :as rf]
    [reitit.frontend.easy :as rfe]))

(defn redirect!
  [to]
  (r/create-class
    {:component-did-mount #(rfe/replace-state to)
     :render (fn [] nil)}))

(def routes
  [["/"
    {:view (redirect! :view.login)}]
   ["/login"
    {:name :view.login
     :view login/view}]])

(def run (rf/router routes {:data {:coercion rcs/coercion}}))

(def match (r/atom nil))
