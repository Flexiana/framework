(ns app
  (:require [controllers.status :as status]
            [corpus.router.reitit :as corpus]
            [corpus.responses.hiccup :as hiccup]))

(def routes
  (concat [""
           ["/status" {:get status/handle-status}]]))

(defn ring-app
      [conf]
      (corpus/ring-handler conf
                           (corpus/router routes conf hiccup/wrap-render)))