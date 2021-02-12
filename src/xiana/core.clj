(ns xiana.core
  (:require
    [cats.core :as m]
    [cats.monad.either :as me]))

(defrecord State [http-request
                  request-data
                  response
                  session-data
                  deps
                  template
                  lang
                  layout
                  is-api
                  is-html
                  ready-hiccup
                  dict-fn])

(def ok me/right)

(def error me/left)

(def extract m/extract)

(defmacro flow->
  [state & forms]
  `(m/>>=
     (ok ~state)
     ~@(for [form forms]
         (if (seq? form)
           `(fn [~'x] (~(first form) ~'x ~@(rest form)))
           form))))
