(ns app.common.components.date-picker)

(def format ::format)

(def component {format "yyyy-mm-dd"})

(defn render [this]
  [:h1 {} (format this)])
