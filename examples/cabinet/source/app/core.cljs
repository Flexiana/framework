(ns app.core
  (:require [reagent.dom :as dom]
            [reitit.frontend.easy :as rfe]
            [app.router :as router]))

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
