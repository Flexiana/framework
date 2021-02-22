(ns framework.app.view.core
  (:require
    [hiccup2.core :as hiccup2]
    [tongue.core :as tongue]
    [xiana.core :as xiana]))

(defn ->html
  [hic]
  (hiccup2/html hic))

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
    (println headers)
    (-> state
        (assoc-in [:lang] (keyword (get headers "accept-language")))
        xiana/ok)))

(defn set-lang
  [state langs]
  (-> state
      (assoc-in [:lang] langs)
      xiana/ok))

(defn set-lang-by-query-params
  [state]
  (let [{{{:keys [language]} :params} :request-data} state]
    (-> state
        (assoc-in [:lang] (keyword language))
        xiana/ok)))

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

(defn set-view
  [state data]
  (-> state
      (assoc :view (partial data))
      xiana/ok))

(defn set-response
  [state resp]
  (-> state
      (assoc :response-fn (partial resp))
      (xiana/ok)))

(defn get-layout-fn
  [state]
  (if-let [layout (:layout state)]
    layout
    (throw (Exception. ">> Layout has not been setted!"))))

(defn get-layout
  [state]
  (let [layout (get-layout-fn state)]
    (try
      (layout)
      (catch Exception e (str ">> Caught Exception: " (.getMessage e))))))

(defn get-view-fn
  [state]
  (if-let [view-data (:view state)]
    view-data
    (throw (Exception. ">> View data not setted!"))))

(defn get-view
  [state]
  (let [view-data (get-view-fn state)]
    (try
      (view-data)
      (catch Exception e (str ">> Caught Exception: " (.getMessage e))))))

(defn generate-response
  [state]
  (let [{:keys [is-html ready-hiccup ready-view response-fn]} state]
    (cond
      is-html (-> state
                  (assoc-in [:response] (-> state
                                            ready-hiccup
                                            ->html
                                            str
                                            response-fn))
                  (xiana/ok))
      is-api (-> state
                 (assoc-in [:response] (response-fn (ready-view)))
                 (xiana/ok))
      :else (-> state
                (xiana/ok)))))

(defn render
  [state]
  (let [{:keys [is-html is-api layout template view]} state]
    (cond
      is-html (-> state
                  (assoc-in [:ready-hiccup] (partial (comp layout template)))
                  (xiana/ok))
      is-api (-> state
                 (assoc-in [:ready-view] (partial view))
                 (xiana/ok))
      :else (-> state
                (assoc-in [:ready-hiccup] nil)
                (xiana/ok)))))
