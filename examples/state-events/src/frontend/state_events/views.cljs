(ns state-events.views
  (:require
    [re-frame.core :as re-frame]
    [state-events.subs :as subs]
    [clojure.string :as str]))

(defonce st (reagent.core/atom {}))

(defn input-gr [atm k]
  [:div.input-group
   [:span.input-group-text (apply str (str/upper-case (first (name k))) (rest (name k)))]
   [:input.form-control {:type "text" :aria-label (name k) :on-change #(swap! st assoc k (-> % .-target .-value))}]
   [:button {:type "button" :label "Last name"
             :on-click #(swap! atm merge {k (get @st k)})} "Add"]])

(defn info-panel [atm]
  [:div {:style {:margin-left 50}}
   [:table {:class :table-bordered
            :style {:min-width 350}}
    [:thead [:tr [:th "Key"] [:th "Value"]]]
    [:tbody (map (fn [[k v]] [:tr [:td k] [:td v]]) @atm)]]
   [:button {:on-click #(reset! atm {})} "Undo"]])

(defn main-panel [atm]
  [:div
   [:div {:class "d-flex flex-row bd-highlight mb-3"
          :style {:margin-left 100
                  :margin-top  100}}
    [:div
     (input-gr atm :first-name)
     (input-gr atm :last-name)
     (input-gr atm :e-mail)
     (input-gr atm :phone)
     (input-gr atm :city)]
    (info-panel atm)]])





