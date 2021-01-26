(ns app
  (:require [controllers.status :as status]
            [controllers.index :as index]
            [corpus.router.reitit :as corpus]
            [corpus.responses.hiccup :as hiccup]
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
