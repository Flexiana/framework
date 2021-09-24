(ns framework.app.view.css.tailwind.helpers
  (:require
    [clojure.string :as s]
    [com.wsscode.tailwind-garden.expanders :as exp]
    [garden.stylesheet]))


(defn strip-whitespace
  [s]
  (cond
    (string? s) (s/join (s/split s #"\s+"))
    :else
    s))


(defn strip-variants-on-keys
  [k]
  (let [thekey k]
    (cond
      (s/starts-with? thekey "hover") (subs thekey 0 (- (count thekey) (count ":hover")))
      (s/starts-with? thekey "focus") (subs thekey 0 (- (count thekey) (count ":focus")))
      (s/starts-with? thekey "active") (subs thekey 0 (- (count thekey) (count ":active")))
      (s/starts-with? thekey "disable") (subs thekey 0 (- (count thekey) (count ":disable")))
      :else
      k)))


(defn garden->map
  [everything]
  (into (hash-map) (for [class everything
                         :let [f (first class)]]
                     (cond
                       (string? f) {(keyword (s/replace (strip-whitespace f) #"\\" "")) class}
                       (keyword? f) {f class}))))


(defn unfold-responsive-selectors
  "The function get the base components without @media query and apply for each css class the requested garden media selector.
  The garden apply css-media selectors with the `garden.types.CSSAtRule` record."
  [breakpoint prefix css-forms]
  (into (hash-map)
        (for [f css-forms]
          {(keyword (s/replace (strip-whitespace (str "." prefix ":" (subs (name (first f)) 1))) #"\\" "")) (garden.stylesheet/at-media {:min-width breakpoint
                                                                                                                                         :screen true}
                                                                                                                                        (into [] (map #(update % 0 exp/prefix-classname (str prefix "\\" ":"))) [f]))})))


(defn extract-garden-element
  [garden-record]
  (into []
        (flatten
          (-> garden-record
              :value
              :rules))))


(defn fold-responsive-selectors
  [classes-used-map size-key size]
  (garden.stylesheet/at-media {:min-width size :screen true}
                              (reduce into [] (for [v (vals (size-key classes-used-map))]
                                                [(extract-garden-element v)]))))


(defn class-keys->classes-string
  ;; the return is a string of the names class keys "class1 class2 class3"
  [in-hiccup-keys]
  (let [set-keys (apply sorted-set (reduce conj #{} in-hiccup-keys))]
    (s/join " " (map (fn [k] (strip-variants-on-keys (subs (name k) 1))) set-keys))))
