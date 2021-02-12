(ns framework.app.view.template
  (:require
    [xiana.core :as xiana]))

(defn set-layout
  [state layout]
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
  (xiana/ok
    (assoc state :template template)))

(defn get-template
  "Return the hiccup function."
  [controller]
  (-> controller
      (xiana/extract)
      (:template)))

(defmulti set-language
  (fn [action] action))

(defmethod set-language :auto
  [state]
  (let [{{:keys [headers]} :http-request} state]
    (xiana/ok
      (assoc state :lang (get headers "accept-language")))))

(defmethod set-language :manual
  [state [& _lang :as langs]]
  (xiana/ok
    (assoc state :lang langs)))
