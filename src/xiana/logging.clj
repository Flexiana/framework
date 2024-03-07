(ns xiana.logging
  (:require
    [taoensso.timbre :as log]))

(defn set-level
  [cfg]
  (when-let [level (-> cfg :logging/timbre-config :min-level)]
    (log/set-min-level! level))
  cfg)
