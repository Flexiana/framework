(ns framework.tailwind.test-tailwind
  (:require
    [clojure.test :refer :all]
    [framework.tailwind.core :as tcore]
    [framework.tailwind.helpers :as thlp]
    [framework.tailwind.preparers :as tprep]
    [framework.tailwind.resolvers :as rlv]
    [garden.core :as gard]
    [hiccup.core :as hiccup]
    [hickory.core :as hick]))

(deftest inline-hiccup
  (let [hiccup-page [:html
                     [:div#thediv {:class (tcore/->hcss* [:.bg-green-500 :.hover:bg-red-500:hover :.sm:bg-yellow-400])}
                      "Hello tailwind"
                      [:button#thebut {:class (tcore/->hcss* [:.bg-red-100 :.hover:bg-red-600:hover])}
                       "button"]]]
        html-page (hiccup/html hiccup-page)]
    (testing "->hcss* result"
      (is (= (tcore/->hcss* [:.bg-pink-100 :.hover:bg-pink-400:hover]) "bg-pink-100 hover:bg-pink-400")))))
