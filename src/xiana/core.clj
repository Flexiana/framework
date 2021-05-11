(ns xiana.core
  (:require
   [cats.core :as m]
   [cats.monad.either :as me]))

;; state/context record definition
(defrecord State [request request-data response session-data deps])

;; monad.either/right container alias
;; don't stop the sequence of executions, continue! (implicit)
(def ok me/right)

;; monad.either/left container alias
;; and stop the sequence of executions (implicit)
;; used by >>= function
(def error me/left)

;; monad.extract alias
;; unwrap monad container
(def extract m/extract)

(defmacro flow->
  "Expand a single form to (form) or the sequence of forms to:
  (lambda (x) (form1 x), (lambda (x) (form2 x)) ...
  and perform Haskell-style left-associative bind using the
  monad.either/right context (wrapped)."
  [state & forms]
  `(m/>>=
    (ok ~state)
    ~@(for [form forms]
        (if (seq? form)
          `(fn [~'x] (~(first form) ~'x ~@(rest form)))
          form))))
