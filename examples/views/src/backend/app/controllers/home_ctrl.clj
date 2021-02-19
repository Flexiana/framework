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
                (xview/is-html)
                (xview/set-lang :en)
                (xview/set-layout lay/layout)
                (xview/set-template home-temp/home)
                (xview/set-dict dicts/home-dict)
                (xview/set-response home-response)
                (xview/render)))
