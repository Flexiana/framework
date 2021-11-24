(ns state-events.core
  (:require
    [re-frame.core :as re-frame]
    [reagent.dom :as rdom]
    [state-events.config :as config]
    [state-events.effects :as effects]
    [state-events.events :as events]
    [state-events.views :as views]))

(defonce atm (reagent.core/atom {}))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel atm] root-el)))

(defn init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
