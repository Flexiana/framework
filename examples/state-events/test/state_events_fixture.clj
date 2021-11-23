(ns state-events-fixture
  (:require
    [state-events.core :refer [->system app-cfg]]))

(defn std-system-fixture
  [f]
  (with-open [_ (->system app-cfg)]
    (f)))

