(ns state-events.views
  (:require
    [clojure.string :as str]
    [re-frame.core :as re-frame]
    [state-events.subs :as subs]))

(defonce st (reagent.core/atom {}))

(defn key->str
  [k]
  (apply str (str/upper-case (first (name k))) (rest (name k))))

(defn input-gr [k]
  (let [selected @(re-frame/subscribe [:selected])]
    [:div.input-group {:key    k
                       :hidden (some? (get selected k))}
     [:span.input-group-text (key->str k)]
     [:input.form-control {:type      "text"
                           :on-change (fn [e] (swap! st assoc k (.. e -target -value)))}]
     [:button {:type     "button" :label (key->str k)
               :disabled (empty? selected)
               :on-click #(re-frame/dispatch [k (get @st k)])} "Add"]]))

(defn info-panel []
  (let [atm @(re-frame/subscribe [:selected])]
    (when atm
      [:div {:style {:margin-left 50}}
       [:table {:class :table-bordered
                :style {:min-width 350}}
        [:thead [:tr [:th "Key"] [:th "Value"]]]
        [:tbody (doall (map (fn [[k v]]
                              ^{:key k}
                              [:tr
                               [:td (key->str k)]
                               [:td v]])
                            atm))]]
       [:button
        {:on-click #(re-frame/dispatch [:selected/clean])}
        "Clean"]
       [:button
        {:on-click #(re-frame/dispatch [:selected/undo])}
        "Undo"]
       [:button
        {:on-click #(re-frame/dispatch [:selected/redo])}
        "Redo"]])))

(defn inputs []
  (let [selected @(re-frame/subscribe [:selected])
        input-groups [:persons/first-name :persons/last-name :persons/e-mail :persons/phone :persons/city]]
    (if selected
      [:div {:style {:width 600}}
       (doall (map input-gr input-groups))])))

(defn list-persons []
  (let [persons (seq @(re-frame/subscribe [:persons]))]
    (when persons
      [:div
       [:table {:class :table-bordered
                :style {:min-width 350}}
        [:tbody (doall (map (fn [[k v]]
                              ^{:key k}
                              [:tr {:on-click #(re-frame/dispatch [:table/click k])}
                               [:td (name k)]
                               [:td (str (:first-name v) " " (:last-name v))]])
                            persons))]]])))

(def new-person
  [:button {:type  "button"
            :style {:width  600
                    :height 58}
            :on-click
            (fn [_]
              (re-frame/dispatch [:persons/create (random-uuid)]))}
   "Create new person resource"])

(defn main-panel []
  [:div
   [:div {:class "d-flex flex-row bd-highlight mb-3"
          :style {:margin 100}}
    [:div
     new-person
     (list-persons)]
    (inputs)
    (info-panel)]])

