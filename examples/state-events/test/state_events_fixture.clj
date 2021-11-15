(ns state-events-fixture
  (:require
    [state-events.core :refer [->system app-cfg]]))

(defn std-system-fixture
  [config f]
  (with-open [_ (->system (merge app-cfg config))]
    (f)))

