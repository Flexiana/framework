(ns xiana.interceptor.kebab-camel
  (:require
    [camel-snake-kebab.core :as csk]
    [camel-snake-kebab.extras :as cske]
    [clojure.core.memoize :as mem]))

(def interceptor
  "The purpose is to make Js request compatible with clojure, and response compatible with Javascript.
  :request - {:params { "
  {:name  ::camel-to-kebab-case
   :enter (fn [state]
            (update-in state [:request :params]
                       (fn [resp]
                         (cske/transform-keys
                           (mem/fifo csk/->kebab-case {} :fifo/threshold 512) resp))))
   :leave (fn [state]
            (update-in state [:response :body]
                       (fn [resp]
                         (cske/transform-keys
                           (mem/fifo csk/->camelCase {} :fifo/threshold 512) resp))))})
