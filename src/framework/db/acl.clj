(ns framework.db.acl
  (:require [clojure.string :as str]))


(defn action-table [query]
  (let [q (str/replace query ";" " ")
        r (map
            (fn [[_ action _ on]] [(keyword (str/lower-case action)) on])
            (or
              (re-seq #"(ALTER|SELECT|INSERT|CREATE|DROP|TRUNCATE|DELETE).*(FROM|INTO|TABLE)\s+(\S*)" q)
              (re-seq #"(UPDATE|TRUNCATE)(\s+)(\S*)" q)))]
    r))

(defn fetch [session key]
  (get session key ""))


(defn user-id [env]
  (:id (fetch (:session env) :user)))

(defn acl [{:keys [user-roles]} query]
  (let [action (action-table query)]
    (println "action: " action "roles" (flatten (vals user-roles)))
    (filter (set action) user-roles)))


(action-table "SELECT * FROM items;")

(def action '([:select "items"] [:delete "items"]))
(def roles [{:select "items", :filter :all} {:select "users", :filter :own} {:update "users", :filter :own} {:delete "users", :filter :own} {:insert "addresses", :filter :own} {:update "addresses", :filter :own} {:select "addresses", :filter :own} {:delete "addresses", :filter :own} {:select "carts", :filter :own} {:insert "carts", :filter :own} {:update "carts", :filter :own} {:delete "carts", :filter :own}])

(filter :select roles)


(map #(dissoc % :filter) roles)
(map first action)