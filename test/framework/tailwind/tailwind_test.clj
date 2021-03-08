(ns framework.tailwind.tailwind-test
  (:require
    [clojure.string :as s]
    [clojure.test :refer [deftest testing is]]
    [framework.app.view.css.tailwind.core :as tcore]
    [framework.app.view.css.tailwind.helpers :as thlp]
    [framework.app.view.css.tailwind.preparers :as tprep]
    [framework.app.view.css.tailwind.resolvers :as trlv]
    [garden.core :as gard]
    [hiccup.core :as hiccup]
    [hickory.core :as hick]))

(defn string->vec
  [s]
  (into [] (s/split s #"\s+")))

(deftest inline-hiccup
  (let [hiccup-page [:html
                     [:div#thediv {:class (tcore/->hcss* (sort [:.bg-green-500 :.hover:bg-red-500:hover :.sm:bg-yellow-400]))}
                      "Hello tailwind"
                      [:button#thebut {:class (tcore/->hcss* (sort [:.bg-red-100 :.hover:bg-red-600:hover]))}
                       "button"]]]
        html-page (hiccup/html hiccup-page)]
    (testing "Testing ->hcss* result"
      (is (= (frequencies (string->vec (tcore/->hcss* [:.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus])))
             (frequencies (string->vec "bg-pink-100 hover:bg-pink-400 focus:bg-pink-400"))))
      (is (= (count (string->vec (tcore/->hcss* [:.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus])))
             (count (string->vec "bg-pink-100 hover:bg-pink-400 focus:bg-pink-400"))))
      (is (= (frequencies (string->vec "bg-pink-100 hover:bg-pink-400 focus:bg-pink-400"))
             (frequencies (string->vec (tcore/->hcss* [:.bg-pink-100 :.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus])))))
      (is (not= (frequencies (string->vec (tcore/->hcss* [:.bg-pink-100 :.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus])))
                (frequencies (string->vec "bg-pink-100 hover:bg-pink-400 bg-pink-100 focus:bg-pink-400"))))
      (is (= (hiccup/html hiccup-page)
             "<html><div class=\"bg-green-500 hover:bg-red-500 sm:bg-yellow-400\" id=\"thediv\">Hello tailwind<button class=\"bg-red-100 hover:bg-red-600\" id=\"thebut\">button</button></div></html>"))
      (testing "Testing css-keys-in-hiccup atom"
        (is (= (sort @tprep/css-keys-in-hiccup) (sort [:.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus
                                                       :.bg-green-500 :.hover:bg-red-500:hover :.sm:bg-yellow-400 :.bg-red-100
                                                       :.hover:bg-red-600:hover])))
        (tcore/->hcss* (sort [:.bg-pink-100 :.bg-purple-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus :.bg-red-100]))
        (is (= (sort @tprep/css-keys-in-hiccup) (sort [:.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus ;; with :.bg-purple-100
                                                       :.bg-green-500 :.hover:bg-red-500:hover :.sm:bg-yellow-400 :.bg-red-100
                                                       :.hover:bg-red-600:hover :.bg-purple-100])))
        (is (not= (sort @tprep/css-keys-in-hiccup) (sort [:.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus ;; without :.bg-purple-100
                                                          :.bg-green-500 :.hover:bg-red-500:hover :.sm:bg-yellow-400 :.bg-red-100
                                                          :.hover:bg-red-600:hover])))
        (is (= (:bases (tcore/result-css-map @tprep/css-keys-in-hiccup))
               {:.bg-pink-100 [:.bg-pink-100 {:--tw-bg-opacity "1", :background-color "rgba(252, 231, 243, var(--tw-bg-opacity))"}],
                :.hover:bg-pink-400:hover [".hover\\:bg-pink-400:hover" {:--tw-bg-opacity "1", :background-color "rgba(244, 114, 182, var(--tw-bg-opacity))"}],
                :.focus:bg-pink-400:focus [".focus\\:bg-pink-400:focus" {:--tw-bg-opacity "1", :background-color "rgba(244, 114, 182, var(--tw-bg-opacity))"}],
                :.bg-green-500 [:.bg-green-500 {:--tw-bg-opacity "1", :background-color "rgba(16, 185, 129, var(--tw-bg-opacity))"}],
                :.hover:bg-red-500:hover [".hover\\:bg-red-500:hover" {:--tw-bg-opacity "1", :background-color "rgba(239, 68, 68, var(--tw-bg-opacity))"}],
                :.bg-red-100 [:.bg-red-100 {:--tw-bg-opacity "1", :background-color "rgba(254, 226, 226, var(--tw-bg-opacity))"}],
                :.hover:bg-red-600:hover [".hover\\:bg-red-600:hover" {:--tw-bg-opacity "1", :background-color "rgba(220, 38, 38, var(--tw-bg-opacity))"}],
                :.bg-purple-100 [:.bg-purple-100 {:--tw-bg-opacity "1", :background-color "rgba(237, 233, 254, var(--tw-bg-opacity))"}]}))))))
