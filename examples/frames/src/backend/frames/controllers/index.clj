(ns frames.controllers.index
  (:require
    [corpus.responses :as responses]
    [ring.util.response :as response]
    [xiana.core :as xiana]))

(defn handle-index
  [state]
  (xiana/ok (assoc state :response
                   (-> "index.html"
                       (response/resource-response {:root "public"})
                       (responses/as-html)))))
