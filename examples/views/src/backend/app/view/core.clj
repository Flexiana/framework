(ns view.core
  (:require
    [hiccup.core :as hiccup]
    [tongue.core :as tongue]
    [xiana.core :as xiana]))

(defn set-layout
  [state layout]
  (println (str ";; Set layout " state))
  (xiana/ok
    (assoc state :layout layout)))

(defn get-layout
  [controller]
  (-> controller
      (xiana/extract)
      (:layout)))

(defn set-template
  "Setting the hiccup template function to state map."
  [state template]
  (println (str ";; Set-temlpate" state))
  (xiana/ok
    (assoc state :template template)))

(defn get-template
  "Return the hiccup function."
  [controller]
  (-> controller
      (xiana/extract)
      (:template)))

(defn set-language-auto
  [state]
  (println (str ";; Set-language-auto" state))
  (let [{{:keys [headers]} :http-request} state]
    (xiana/ok
      (assoc state :lang (keyword (get headers "accept-language"))))))

(defn set-language-manually
  [state [& lang :as langs]]
  (xiana/ok
    (assoc state :lang langs)))

(defn response-html
  [state]
  (println (str ";;Response-html" state))
  (xiana/ok
    (assoc state :is-html true)))

(defn response-api
  [state]
  (assoc state :is-api true))

(defn ->html
  [hic]
  (hiccup/html hic))

(defn set-dictionary
  [state dict-map]
  (let [dfn (tongue/build-translate dict-map)]
    (xiana/ok
      (assoc state :dict-fn dfn))))

(defn set-response
  [state response & rest]
  (let [{:keys [ready-hiccup is-html is-api]} state]
    (if is-html
      (xiana/ok
        (assoc state :response (response (->html ready-hiccup)))))))

;; (defn generate-response
;;   [{:keys [is-html is-api response ready-hiccup] :as state}]
;;   (xiana/ok
;;    (cond
;;     (true? is-html) (update state :response (response (constantly (->html ready-hiccup)))))))


(defn render-html-view
  [state]
  (let [{:keys [layout template]} state
        ;; hic ((comp layout template) state)
        ]
    (xiana/ok
      (assoc state :ready-hiccup (layout (template state))))))

(defn render
  [state]
  (let [{:keys [is-api is-html]} state]
    (if is-html
      (render-html-view state)
      (identity state))))
