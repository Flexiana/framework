(ns controllers.actions
  (:require [cats.core :as m]
            [cats.monad.either :as me]))

(def ok me/right)

(def error me/left)

(defmacro controller->
  [req & forms]
  `(m/extract (m/>>= (ok ~req)
                     ~@(for [form forms]
                         (if (seq? form)
                           `(fn [~'x] (~(first form) ~'x ~@(rest form)))
                           form)))))

(defn require-logged-in
  [[req resp ctx]]
  (if-let [authorization (get-in req [:headers "authorization"])]
    (ok [req resp (assoc ctx :authorization authorization)])
    (error {:status 401 :body "Unauthorized"})))

(defn index-view
  [[req resp ctx]]
  (println :index-view req resp ctx)
  (ok {:status  200
       :headers {"Content-Type" "text/plain"}
       :body    "Index page"}))

(defn index-action
  [req res ctx]
  (controller-> [req res ctx]
                (require-logged-in)
                (index-view)))
