(ns view.templates.home)

(def d (java.util.Date.))

(defn home
  [state]
  (let [{:keys [dict-fn lang]} state
        l (if (vector? lang)
            (first lang)
            lang)]
    [:div.container
     [:div.column.u-full-width
      {:style {:text-align "center"}}
      [:h1
       (dict-fn l :greet-title "Xiana framework")]
      [:hr]
      [:div.row.u-full-width
       [:a#fr.button {:href "/fr"} "fr"]
       [:a#en.button {:href "/en"} "en"]]
      [:div#date
       (dict-fn l :date d)]]]))
