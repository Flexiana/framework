(ns view.templates.home)

(def d (java.util.Date.))

(defn home
  [state]
  (let [{:keys [dict-fn lang]} state]
    [:div.container
     [:div.column.u-full-width
      {:style {:text-align "center"}}
      [:h1
       (dict-fn lang :greet-title "Xiana framework")]
      [:hr]
      (->> (dict-fn lang :date d)
           (dict-fn lang :present-date))]]))
