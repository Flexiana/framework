(ns state-events.views
  (:require
   [re-frame.core :as re-frame]
   [state-events.subs :as subs]))


(defn main-panel [atm]
  (let [name (re-frame/subscribe [::subs/name])]
    [:div {:style {:width "500px"
                   :margin :auto}}
     [:label "Add user"]
     [:p]
     [:form
      [:div.mb-3
       [:label.form-label {:for "exampleInputEmail1"} "Email address"]
       [:input#exampleInputEmail1.form-control {:type "email" :aria-describedby "emailHelp"}]
       [:div#emailHelp.form-text "We'll never share your email with anyone else."]]
      [:div.mb-3
       [:label.form-label {:for "exampleInputPassword1"} "Password"]
       [:input#exampleInputPassword1.form-control {:type "password"}]]
      [:div.mb-3.form-check
       [:input#exampleCheck1.form-check-input {:type "checkbox"}]
       [:label.form-check-label {:for "exampleCheck1"} "Check me out"]]
      [:button.btn.btn-primary {:type "submit"} "Submit"]]]))


