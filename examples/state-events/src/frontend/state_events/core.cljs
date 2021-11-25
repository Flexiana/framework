(ns state-events.core
  (:require
    [cljs.reader :as edn]
    [re-frame.core :as re-frame]
    [reagent.dom :as rdom]
    [state-events.config :as config]
    [state-events.effects :as effects]
    [state-events.events :as events]
    [state-events.subs :as subs]
    [state-events.views :as views]
    [state-events.web-sockets :as ws]))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (ws/connect! (str "ws://" (.-host js/location) "/sse") ws/handle-response!)
  (dev-setup)
  (mount-root))
