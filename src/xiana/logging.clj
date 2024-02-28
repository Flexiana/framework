(ns xiana.logging
  (:require
   [taoensso.timbre :as timbre]
   [xiana.config :as config]))

(defn set-level
  [cfg]
  (when-let [level (-> (config/config) :logging/timbre-config :min-level)]
    (timbre/set-min-level! level))
  cfg)
