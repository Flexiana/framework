(ns view.templates.layout)

(defn layout
  [body]
  [:html
   [:head
    [:title "Xiana"]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1"}]
    [:link {:rel "stylesheet"
            :href "https://cdnjs.cloudflare.com/ajax/libs/skeleton/2.0.4/skeleton.min.css"}]]
   [:body body]
   [:footer]])
