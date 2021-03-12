(ns app.common.components.date-picker
  (:require
    [goog.date :as date]
    [reagent.core :as r]))

(def ^:private state
  (r/atom {::show? false
           ::current {::day 1
                      ::year 2021
                      ::month 1}}))

(def ^:private date (r/atom nil))

(def ^:private days ["Sunday" "Monday" "Tuesday" "Wednesday" "Thursday" "Friday" "Saturday"])

(def ^:private months
  ["January" "February" "March" "April" "May" "June" "July"
   "August" "September" "October" "November" "December"])

(def on-input ::on-input)

(def component
  {::placeholder "yyyy / mm / dd"})

(defn ^:private print-output
  [f]
  (let [->format #(if (< % 10) (str "0" %) %)
        year (get-in @state [::current ::year])
        day (-> @state (get-in [::current ::day]) ->format)
        month (-> @state (get-in [::current ::month]) inc ->format)
        output (str year " / " month " / " day)]
    (f output)
    (reset! date output)))

(defn ^:private v-slider
  [f]
  (let [btn-style "font-mono font-black text-sm cursor-pointer bg-red-400 h-8 w-8
                   rounded-full flex justify-center items-center flex-none text-white"
        month-index (get-in @state [::current ::month])
        safe-month #(mod % 12)
        modify #(update-in %1 [::current %2] %3)
        month-- #(modify % ::month (comp safe-month dec))
        month++ #(modify % ::month (comp safe-month inc))
        year-- #(modify % ::year dec)
        year++ #(modify % ::year inc)]
    [:div {:class "flex items-center font-mono mb-4"}
     [:span {:class btn-style :on-click (fn []
                                          (swap! state year--)
                                          (print-output f))} "<<"]
     [:span {:class (str btn-style " ml-3") :on-click (fn []
                                                        (swap! state month--)
                                                        (print-output f))} "<"]
     [:div {:class "flex-grow"}
      [:div {:class "flex flex-col"}
       [:span {:class "text-3xl text-center"} (get-in @state [::current ::year])]
       [:span {:class "text-center"} (get months month-index)]]]
     [:span {:class (str btn-style " mr-3") :on-click (fn []
                                                        (swap! state month++)
                                                        (print-output f))} ">"]
     [:span {:class btn-style :on-click (fn []
                                          (swap! state year++)
                                          (print-output f))} ">>"]]))

(defn ^:private v-card
  [f]
  (let [year (get-in @state [::current ::year])
        month (get-in @state [::current ::month])
        set-day #(assoc-in %2 [::current ::day] %1)]
    [:div {:class "float-left absolute left-0 top-4 w-96 h-96 shadow-lg
                   rounded-xl mt-8 px-8 py-4 overflow-hidden"}
     (v-slider f)
     [:div {:class "grid grid-cols-7 gap-4"}
      (for [name-day (map #(subs % 0 3) days)] ^{:key name-day}
           [:div {:class "text-xs font-bold text-center"} name-day])
      (for [day (range 1 (+ 1 (date/getNumberOfDaysInMonth year month)))] ^{:key day}
           [:div
            {:class "cursor-pointer font-mono bg-gray-100 h-8 w-8 rounded-full
                     hover:bg-red-400 hover:text-white flex justify-center items-center"
             :on-click (fn []
                         (swap! state (partial set-day day))
                         (swap! state #(assoc % ::show? false))
                         (print-output f))}
            day])]]))

(defn ^:private view
  [this]
  [:div {:class "float-left relative"}
   [:div {:class "float-left w-60 h-10 overflow-hidden rounded-full"
          :on-click (fn [] (swap! state #(assoc % ::show? true)))}
    [:input {:type "text"
             :value @date
             :placeholder (::placeholder this)
             :on-change #(let [f (on-input this)] (print-output f))
             :class "uppercase text-sm text-gray-400 font-mono border-0 w-full h-full
                     bg-gray-100 text-center focus:outline-none"}]]
   (when (::show? @state) (v-card (on-input this)))])

(defn ^:private add-dom-click
  []
  (.addEventListener js/window "click"
                     (when (::show? @state)
                       (swap! state #(assoc % ::show? false)))))

(defn ^:private remove-dom-click
  []
  (.removeEventListener js/window "click"
                        (swap! state #(assoc % ::show? true))))

(defn render
  [this]
  [:> (r/create-class
        {:display-name "picker"
         :component-did-mount add-dom-click
         :component-will-mount remove-dom-click
         :render #(view this)})])
