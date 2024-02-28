(ns xiana.logging
  (:require
    [taoensso.timbre :as log]
    [xiana.config :as config]))

(defn set-level
  [cfg]
  (when-let [level (-> (config/config) :logging/timbre-config :min-level)]
    (log/set-min-level! level))
  cfg)
