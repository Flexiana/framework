(ns view.core
  (:require
    [hiccup.core :as hiccup]
    [tongue.core :as tongue]
    [xiana.core :as xiana]))

(defn ->html
  [hic]
  (hiccup/html hic))

(defmulti view
  (fn [state action & rest]
    action))

(defmethod view :set-template
  [state _ template]
  (-> state
      (assoc :template (partial template))
      xiana/ok))

(defmethod view :set-layout
  [state _ layout]
  (-> state
      (assoc :layout (partial layout))
      xiana/ok))

(defmethod view :auto-set-lang
  [state _]
  (let [{{:keys [headers]} :http-request} state]
    (-> state
        (assoc-in [:lang] (keyword (get headers "accept-language")))
        xiana/ok)))

(defmethod view :set-lang
  [state _ [& lang :as langs]]
  (-> state
      (assoc :lang langs)
      xiana/ok))

(defmethod view :is-html
  [state _]
  (-> state
      (assoc :is-html true)
      xiana/ok))

(defmethod view :is-api
  [state _]
  (-> state
      (assoc :is-api true)
      xiana/ok))

(defmethod view :set-dict
  [state _ dict-map]
  (let [dfn (tongue/build-translate dict-map)]
    (-> state
        (assoc :dict-fn (partial dfn))
        xiana/ok)))

(defmethod view :set-view-data
  [state _ data]
  (-> state
      (assoc :view-data (partial data))
      xiana/ok))

(defmethod view :set-response
  [state _ resp]
  (-> state
      (assoc :response-fn (partial resp))
      (xiana/ok)))

(defmethod view :generate-response
  [state _ & data]
  (let [{:keys [is-html ready-hiccup response-fn]} state]
    (cond
      is-html (-> state
                  (assoc-in [:response] (-> state
                                            ready-hiccup
                                            ->html
                                            response-fn))
                  (xiana/ok))
          ;;TODO is-api
      :else (-> state
                (xiana/ok)))))

(defmethod view :render
  [state _]
  (let [{:keys [is-html is-api layout template]} state]
    (cond
      is-html (-> state
                  (assoc-in [:ready-hiccup] (partial (comp layout template)))
                  (xiana/ok))
      :else (-> state
                (assoc-in [:ready-hiccup] nil)
                (xiana/ok)))))
