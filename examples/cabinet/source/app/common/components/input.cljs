(ns app.common.components.input)

(def component {::help nil
                ::size "small"
                ::block? false
                ::type "text"
                ::placeholder ""})

(defn set-size [size]
  (let [size? (some #{size} ["large" "medium" "small"])
        size' (if size? size "medium")]
    #(assoc % ::size size')))

(defn set-placeholder [s]
  #(assoc % ::placeholder s))

(defn set-type [s]
  #(assoc % ::type s))

(defn- style [_]
  "p-3 mb-6 w-full border shadow text-blue-500 appearance-none
   focus:outline-none focus:shadow-outline leading-tight rounded-full
   text-center font-bold")

(defn render [events]
  #(let [style' (style (get % ::size))
         attributes (merge events {:class style'
                                   :type (get % ::type)
                                   :placeholder (get % ::placeholder)})]
     [:input attributes]))