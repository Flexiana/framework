(ns app
  (:require
    [controllers.index :as index]
    [controllers.status :as status]
    [corpus.responses.hiccup :as hiccup]
    [corpus.router.reitit :as corpus]
    [reitit.ring :as ring]))


(def routes
  (concat [""
           ["/" {:get index/handle-index}]
           ["/status" {:get status/handle-status}]
           ["/assets/*" (ring/create-resource-handler)]]))


(defn ring-app
  [conf]
  (corpus/ring-handler conf
                       (corpus/router routes conf hiccup/wrap-render)))
