(ns framework.app.view.core
  (:require
    [hiccup.core :as hiccup]
    [tongue.core :as tongue]
    [xiana.core :as xiana]))

(defn ->html
  [hic]
  (hiccup/html hic))

(defn set-dictionary
  [state dict-map]
  (xiana/ok
    (assoc state :dict-fn (tongue/build-translate dict-map))))

(defn set-response
  [state response]
  (xiana/ok
    (assoc state :response response)))

(defmulti render-view
  (fn [action] action))

(defmethod render-view :is-html
  [{:keys [layout template lang dict-fn]} _state]
  (comp layout (template dict-fn lang)))

(defmethod render-view :is-api
  [state]
  ;; TODO
  (identity state))

(defn render
  [state]
  (let [{:keys [is-api is-html]} state]
    (cond->
      (is-html) (render-view :is-html state)
      (is-api) (render-view :is-api state))))
