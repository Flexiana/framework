(ns controllers.records-ctrl
  (:require
    [view.core :as xview]
    [view.templates.dictionaries :as dicts]
    [view.templates.layout :as lay]
    [view.templates.records :as record-temp]
    [xiana.core :as xiana]))

(defn record-response
  [body]
  {:status 200
   :headers {"Content-Type" "text/html"
             "charset" "UTF-8"}
   :body body})

(def a-date (java.util.Date.))

(defn dummy-data
  []
  [{:first-name "first-name-1" :last-name "last-name-1" :age 30 :date a-date}
   {:first-name "first-name-2" :last-name "last-name-2" :age 31 :date a-date}
   {:first-name "first-name-3" :last-name "last-name-3" :age 40 :date a-date}])

(defn records-controller
  [state]
  (xiana/flow-> state
                (xview/is-html)
                (xview/set-lang-by-query-params)
                (xview/set-layout lay/layout)
                (xview/set-template record-temp/records)
                (xview/set-dict dicts/records-dict)
                (xview/set-response record-response)
                (xview/set-view-data (dummy-data))
                (xview/render)))
