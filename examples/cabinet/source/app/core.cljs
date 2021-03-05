(ns app.core
  (:require
    [app.router :as router]
    [reagent.dom :as dom]
    [reitit.frontend.easy :as rfe]))

(defn app
  []
  (when @router/match
    (let [view (get-in @router/match [:data :view])]
      [view @router/match])))

(defn ^:export ^:dev/after-load main
  []
  (rfe/start! router/run
              #(reset! router/match %)
              {:use-fragment false})
  (dom/render [app] (js/document.getElementById "app")))
