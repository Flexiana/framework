(ns framework.interceptor.debug
  (:require
    [taoensso.timbre :as log]))

(defn keyceptor
  [& keyz]
  (let [logger (fn [state]
                 (log/info keyz (get-in state keyz))
                 state)]
    {:name  ::keyceptor
     :enter logger
     :leave logger
     :error logger}))
