(ns app.common.components.date-picker
  (:require [goog.date :as date]))

(def ^:private one-day
  (* 60 60 24 1000))

(def ^:private today-timestamp
  (let [a (.now js/Date)
        b (mod (.now js/Date) one-day)
        c (* (.getTimezoneOffset (js/Date.)) 1000 60)]
    (- a (+ b c))))

(def format ::format)

(def component
  {format "yyyy-mm-dd"})

(defn render
  [this]
  [:div {:class "float-left relative"}
   [:div {:class "float-left w-60 h-10 overflow-hidden rounded-full"}
    [:input {:type "text"
             :class "uppercase border-0 w-full h-full bg-gray-100 text-center"
             :value (format this)}]]
   [:div {:class "float-left absolute left-0 top-4 w-96 h-96 shadow-lg
                  rounded-xl mt-8 p-8 overflow-hidden"}
    [:div {:class "grid grid-cols-7 gap-3"}
     (for [day (range 1 (+ 1 (date/getNumberOfDaysInMonth 2021 3)))]
       [:div
        {:class "cursor-pointer bg-gray-100 h-8 w-8 rounded-full flex justify-center items-center"}
        day])]]])
