(ns app.common.components.button)

(def component
  {::label nil
   ::block? false
   ::mode "ready"})

(defn set-mode
  [mode]
  (let [mode? (some #{mode} ["loading" "disabled" "ready"])
        mode' (if mode? mode "ready")]
    #(assoc % ::mode mode')))

(defn set-label
  [s]
  #(assoc % ::label s))

(defn- style
  [mode]
  (cond
    (= mode "ready") "text-white bg-red-500 font-bold rounded-full w-full h-12"
    :else "flex justify-center items-center border-b-4 bg-gray-300 border-gray-400
           cursor-not-allowed font-bold rounded-full w-full h-12"))

(defn render
  [events]
  #(let [mode (get % ::mode)
         label (get % ::label)
         style' (style mode)
         attributes (merge events {:class style'})]
     (cond
       (= mode "ready") [:button attributes label]
       :else [:div attributes label])))
