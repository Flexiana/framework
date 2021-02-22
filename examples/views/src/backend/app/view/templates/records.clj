(ns view.templates.records)

(defn records
  [state]
  (let [{:keys [dict-fn lang view]} state
        {{:keys [records]} :data} view]
    [:div {:class "container"}
     [:div.column.u-full-width
      {:style {:text-align "center"}}
      [:h2
       (dict-fn lang :table-title)]
      [:table {:class "u-full-width"}
       [:thead
        [:tr
         [:th (dict-fn lang :header-first-name)]
         [:th (dict-fn lang :header-last-name)]
         [:th (dict-fn lang :header-age)]
         [:th (dict-fn lang :header-date)]]]
       [:tbody
        (for [rec records]
          [:tr {:style "font-weight:lighter"}
           [:th (:first-name rec)]
           [:th (:last-name rec)]
           [:th (:age rec)]
           [:th (->> (:date rec)
                     (dict-fn lang :date))]])]]]]))
