(ns framework.app.view.core
  (:require
    [hiccup.core :as hiccup]
    [tongue.core :as tongue]
    [xiana.core :as xiana]))

(defn ->html
  [hic]
  (hiccup/html hic))

(defn set-template
  [state template]
  (-> state
      (assoc :template (partial template))
      xiana/ok))

(defn set-layout
  [state layout]
  (-> state
      (assoc :layout (partial layout))
      xiana/ok))

(defn auto-set-lang
  [state]
  (let [{{:keys [headers]} :http-request} state]
    (-> state
        (assoc-in [:lang] (keyword (get headers "accept-language")))
        xiana/ok)))

(defn set-lang
  [state [& lang :as langs]]
  (-> state
      (assoc :lang langs)
      xiana/ok))

(defn is-html
  [state]
  (-> state
      (assoc :is-html true)
      xiana/ok))

(defn is-api
  [state]
  (-> state
      (assoc :is-api true)
      xiana/ok))

(defn set-dict
  [state dict-map]
  (let [dfn (tongue/build-translate dict-map)]
    (-> state
        (assoc :dict-fn (partial dfn))
        xiana/ok)))

(defn set-view-data
  [state data]
  (-> state
      (assoc :view-data (partial data))
      xiana/ok))

(defn set-response
  [state resp]
  (-> state
      (assoc :response-fn (partial resp))
      (xiana/ok)))

(defn generate-response
  [state & data]
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

(defn render
  [state]
  (let [{:keys [is-html is-api layout template]} state]
    (cond
      is-html (-> state
                  (assoc-in [:ready-hiccup] (partial (comp layout template)))
                  (xiana/ok))
      :else (-> state
                (assoc-in [:ready-hiccup] nil)
                (xiana/ok)))))
