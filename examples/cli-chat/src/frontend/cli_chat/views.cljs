(ns cli-chat.views
  (:require
    [cli-chat.subs :as subs]
    [re-frame.core :as re-frame]))

(defn main-panel []
  (let [name (re-frame/subscribe [::subs/name])]
    [:div
     [:h1 "Hello from " @name]]))
