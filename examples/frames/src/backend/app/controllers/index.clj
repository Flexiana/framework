(ns controllers.index
  (:require [corpus.responses :as responses]
            [ring.util.response :as response]))

(defn handle-index
      [& _args]
      (-> "index.html"
          (response/resource-response {:root "public"})
          (responses/as-html)))