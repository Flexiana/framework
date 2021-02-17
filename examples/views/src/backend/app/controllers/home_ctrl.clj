(ns controllers.home-ctrl
  (:require
    [view.core :as xview]
    [view.templates.dictionaries :as dicts]
    [view.templates.home :as home-temp]
    [view.templates.layout :as lay]
    [xiana.core :as xiana]))

(defn home-response
  [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body body})

(defn home-controller
  [state]
  (xiana/flow-> state
                (xview/view :is-html)
                (xview/view :auto-set-lang)
                (xview/view :set-layout lay/layout)
                (xview/view :set-template home-temp/home)
                (xview/view :set-dict dicts/home-dict)
                (xview/view :set-response home-response)
                (xview/view :render)))
