(ns app.router
  (:require [reagent.core :as r]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.coercion.spec :as rcs]
            [app.welcome.views.login :as login]))

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
