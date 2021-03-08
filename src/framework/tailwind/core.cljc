(ns framework.tailwind.core
  (:refer-clojure :exclude [bases])
  (:require
    [clojure.set :as st]
    [clojure.string :as s]
    [garden.core :as garden]
    [garden.def :refer [defstyles]]
    [garden.stylesheet]
    [com.wsscode.tailwind-garden.expanders :as exp]
    [framework.tailwind.helpers :as hlp]
    [framework.tailwind.preparers :as prep]
    [framework.tailwind.resolvers :as rlv]))



(defn- result-css-map
  [user-col]
  (-> (hash-map)
      (assoc-in [:defaults] (-> rlv/smart-css-map
                                :default-components))
      (assoc-in  [:bases] (select-keys (-> rlv/smart-css-map
                                           :bases) user-col))
      (assoc-in [:bases:sm] (select-keys (-> rlv/smart-css-map
                                             :bases:sm) user-col))
      (assoc-in [:bases:md] (select-keys (-> rlv/smart-css-map
                                             :bases:md) user-col))
      (assoc-in [:bases:lg] (select-keys (-> rlv/smart-css-map
                                             :bases:lg) user-col))
      (assoc-in [:bases:xl] (select-keys (-> rlv/smart-css-map
                                             :bases:xl) user-col))
      (assoc-in [:bases:2xl] (select-keys (-> rlv/smart-css-map
                                              :bases:2xl) user-col))
      (assoc-in [:user-css] (select-keys (-> rlv/smart-css-map
                                             :user-css) user-col))))

(defn- update-hiccup-css-keys-atom*
  "A helper function to mutate the inline hiccup classes."
  [[& classes]]
  (doseq [k classes]
    (swap! prep/css-keys-in-hiccup conj k)))

(defn ->hcss*
  "A function that is used to gather the class keys from hiccup, and add them
  in the `.preparers/css-keys-in-hiccup` atom which contain a set. Return the
  string to be placed in the (:class 'class1 class2')"
  [[& classes]]
  (if-not (empty? classes)
    (do
      (update-hiccup-css-keys-atom* classes)
      (hlp/class-keys->classes-string classes))
    ""))

(defn- update-user-css-atom*
  "A helper function to mutate the user-css atom."
  [gform-map]
  (swap! prep/user-css conj gform-map))

(defn ->css*
  "A function that is used as wraper of garden forms, work similar to 'h-tail*'
  the garden forms should be inside a vector, e.g [[:.classA {:width '100'}]]."
  [& garden-form]
  (doseq [f garden-form
          :let [rsl (hlp/garden->map f)
                k (vec (keys rsl))]]
    (update-user-css-atom* rsl)
    (update-hiccup-css-keys-atom* k)))

(defn- group-sm:queries
  [{:keys [bases:sm]}]
  (if-not (empty? bases:sm)
    (garden.stylesheet/at-media {:screen true
                               :min-width "640px"}
                              (reduce into [] (for [v (vals bases:sm)]
                                                [(hlp/extract-garden-element v)])))))

(defn- group-md:queries
  [{:keys [bases:md]}]
  (if-not (empty? bases:md)
    (garden.stylesheet/at-media {:min-width "768px"
                               :screen true}
                              (reduce into [] (for [v (vals bases:md)]
                                                [(hlp/extract-garden-element v)])))))

(defn- group-lg:queries
  [{:keys [bases:lg]}]
  (if-not (empty? bases:lg)
    (garden.stylesheet/at-media {:screen true :min-width "1024px"}
                              (reduce into [] (for [v (vals bases:lg)]
                                                [(hlp/extract-garden-element v)])))))

(defn- group-xl:queries
  [{:keys [bases:xl]}]
  (if-not (empty? bases:xl)
    (garden.stylesheet/at-media {:screen true :min-width "1280px"}
                              (reduce into [] (for [v (vals bases:xl)]
                                                [(hlp/extract-garden-element v)])))))

(defn- group-2xl:queries
  [{:keys [bases:2xl]}]
  (if-not (empty? bases:2xl)
    (garden.stylesheet/at-media {:min-width "1536px"}
                              (reduce into [] (for [v (vals bases:2xl)]
                                                [(hlp/extract-garden-element v)])))))

(defn- state-usr-classes
  "Evaluate the css functions of css classes to add the garden forms
  in the user-css atom. Accepts a vector of functions"
  [& fns]
  (apply eval fns))

(defn- ->garden
  ([]
   (let [col (vec @prep/css-keys-in-hiccup)
         res-css-map (result-css-map col)
         defaults (:defaults res-css-map)
         bases (vals (:bases res-css-map))
         usr (vals (:user-css res-css-map))
         animation (-> rlv/smart-css-map :animation)
         container (-> rlv/smart-css-map :container)]
     (conj
      defaults
      bases
      usr
      container
       (-> res-css-map
           (group-sm:queries))
       (-> res-css-map
           (group-md:queries))
       (-> res-css-map
           (group-lg:queries))
       (-> res-css-map
           (group-xl:queries))
       (-> res-css-map
           (group-2xl:queries))
       animation
       )))
  ([coll]
   (let [col (st/union (set coll) @prep/css-keys-in-hiccup)
         res-css-map (result-css-map col)
         defaults (:defaults res-css-map)
         bases (vals (:bases res-css-map))
         usr (vals (:user-css res-css-map))
         animation (-> rlv/smart-css-map :animation)
         container ((-> rlv/smart-css-map :container))]
     (conj
      defaults
      bases
      usr
      container
       (-> res-css-map
           (group-sm:queries))
       (-> res-css-map
           (group-md:queries))
       (-> res-css-map
           (group-lg:queries))
       (-> res-css-map
           (group-xl:queries))
       (-> res-css-map
           (group-2xl:queries))
       animation))))

(defmulti garden-to
  (fn [action & col] action))

(defmethod garden-to :css
  ([_ [& :as key-col]]
   (garden.core/css (->garden key-col)))
  ([_]
   (garden.core/css (->garden))))

(defmethod garden-to :file
  ([_ [& :as key-col] ^String file-name]
   (spit (str file-name ".css") (garden.core/css (->garden key-col))))
  ([_ ^String file-name]
   (spit (str file-name ".css") (garden.core/css (->garden)))))
