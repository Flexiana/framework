(ns xiana.interceptor.kebab-camel
  (:require
    [camel-snake-kebab.core :as csk]
    [camel-snake-kebab.extras :as cske]
    [clojure.core.memoize :as mem]))

(def request-type-params [:params :body-params :query-params :path-params :form-params :multipart-params])

(def camel-to-kebab
  (fn [resp]
    (cske/transform-keys
      (mem/fifo csk/->kebab-case {} :fifo/threshold 512) resp)))

(def kebab-to-camel
  (fn [resp]
    (cske/transform-keys
      (mem/fifo csk/->camelCase {} :fifo/threshold 512) resp)))

(def interceptor
  "The purpose is to make Js request compatible with clojure, and response compatible with Javascript.
  :request - {:params { "
  {:name  ::camel-to-kebab-case
   :enter (fn [state]
            (reduce
              (fn [state type-param]
                (update-in state [:request type-param] camel-to-kebab))
              state
              request-type-params))
   :leave (fn [state]
            (update-in state [:response :body] kebab-to-camel))})
