(ns state-events.views
  (:require
    [clojure.string :as str]
    [re-frame.core :as re-frame]
    [state-events.subs :as subs]))

(defonce st (reagent.core/atom {}))

(defn key->str
  [k]
  (apply str (str/upper-case (first (name k))) (rest (name k))))

(defn input-gr [atm k]
  [:div.input-group {:key k
                     :hidden  (some? (@atm k))}
   [:span.input-group-text (key->str k)]
   [:input.form-control {:type      "text"
                         :value     (get @st k)
                         ;; :aria-label (name k)
                         :on-change (fn [e] (swap! st assoc k (.. e -target -value)))}]
   [:button {:type     "button" :label "Last name"
             :on-click #(swap! atm merge {k (get @st k)})} "Add"]])

(defn info-panel [atm]
  [:div {:style {:margin-left 50}}
   [:table {:class :table-bordered
            :style {:min-width 350}}
    [:thead [:tr [:th "Key"] [:th "Value"]]]
    [:tbody  (doall (map (fn [[k v]]
                           ^{:key k}
                           [:tr
                            [:td (key->str k)]
                            [:td v]])
                         @atm))]]
   [:button
    {:on-click (fn [_]
                 (reset! atm {})
                 (reset! st {}))}
    "Clean"]
   [:button
    {:on-click (fn [_]
                 (reset! atm {})
                 (reset! st {}))}
    "Undo"]])

(defn inputs [atm]
  (let [input-groups [:first-name :last-name :e-mail :phone :city]]
    [:div {:style {:width 600}}
     (if (:uuid @atm)
       (doall (map (partial input-gr atm) input-groups))
       [:button {:type     "button"
                 :style {:width 600
                         :height 58}
                 :on-click
                 (fn [_]
                   (swap! atm assoc :uuid (random-uuid)))}
        "Create new person resource"])]))

(defn main-panel [atm]
  [:div

   [:div {:class "d-flex flex-row bd-highlight mb-3"
          :style {:margin 100}}
    (inputs atm)

    (info-panel atm)]])

