(ns framework.app.view.css.tailwind.core-test
  (:require
    [clojure.string :as s]
    [clojure.test :refer [deftest testing is use-fixtures]]
    [framework.app.view.css.tailwind.core :as tcore]
    [framework.app.view.css.tailwind.helpers :as thlp]
    [framework.app.view.css.tailwind.preparers :as tprep]
    [framework.app.view.css.tailwind.resolvers :as trlv]
    [garden.core :as gard]
    [garden.stylesheet :as gst]
    [hiccup.core :as hiccup]
    [hickory.core :as hick]))

(defn string->vec
  [s]
  (into [] (s/split s #"\s+")))

(defn atom-reseter-fixture
  [f]
  (try
    (f)
    (finally
      (do (reset! tprep/css-keys-in-hiccup #{})
          (reset! tprep/user-css {})))))

(use-fixtures :each atom-reseter-fixture)

(deftest using-in-hiccup->hcss*
  (let [hiccup-page [:html
                     [:div#thediv {:class (tcore/->hcss*
                                           [:.bg-green-500 :.hover:bg-red-500:hover :.sm:bg-yellow-400])}
                      "Hello tailwind"
                      [:button#thebut {:class (tcore/->hcss*
                                               [:.bg-red-100 :.hover:bg-red-600:hover])}
                       "button"]]]
        html-page   (hiccup/html hiccup-page)]
    (is (= html-page
           "<html><div class=\"bg-green-500 hover:bg-red-500 sm:bg-yellow-400\" id=\"thediv\">Hello tailwind<button class=\"bg-red-100 hover:bg-red-600\" id=\"thebut\">button</button></div></html>"))))

(deftest result-css-map
  (let [_ (doall
           [(tcore/->hcss* [:.bg-green-500 :.hover:bg-red-500:hover :.sm:bg-yellow-400])
            (tcore/->hcss* [:.bg-red-100 :.hover:bg-red-600:hover])
            (tcore/->hcss* [:.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus])
            (tcore/->hcss* [:.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus])
            (tcore/->hcss* [:.bg-pink-100 :.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus])
            (tcore/->hcss* [:.bg-pink-100 :.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus])])]
    (is (= {:defaults
            [["*" "*::before" "*::after" {:box-sizing "border-box"}]
             [":root" {:-moz-tab-size "4", :tab-size "4"}]
             ["html" {:line-height "1.15", :-webkit-text-size-adjust "100%"}]
             ["body" {:margin "0"}]
             ["body"
              {:font-family
               "system-ui, -apple-system, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji';"}]
             ["hr" {:height "0", :color "inherit"}]
             ["abbr[title]"
              {:-webkit-text-decoration "underline dotted",
               :text-decoration         "underline dotted"}]
             ["b" "strong" {:font-weight "bolder"}]
             ["code"
              "kbd"
              "samp"
              "pre"
              {:font-family
               "ui-monospace, SFMono-Regular, Consolas, 'Liberation Mono', Menlo, monospace",
               :font-size "1em"}]
             ["small" {:font-size "80%"}]
             ["sub"
              "sup"
              {:font-size      "75%",
               :line-height    "0",
               :position       "relative",
               :vertical-align "baseline"}]
             ["sub" {:bottom "-0.25em"}]
             ["sup" {:top "-0.5em"}]
             ["table" {:text-indent "0", :border-color "inherit"}]
             ["button"
              "input"
              "optgroup"
              "select"
              "textarea"
              {:font-family "inherit",
               :font-size   "100%",
               :line-height "1.15",
               :margin      "0"}]
             ["button" "select" {:text-transform "none"}]
             ["button"
              "[type='button']"
              "[type='reset']"
              "[type='submit']"
              {:-webkit-appearance "button"}]
             ["::-moz-focus-inner" {:border-style "none", :padding "0"}]
             [":-moz-focusring" {:outline "1px dotted ButtonText"}]
             [":-moz-ui-invalid"]
             {:box-shadow "none"}
             ["legend"]
             {:padding "0"}
             ["progress"]
             {:vertical-align "baseline"}
             ["::-webkit-inner-spin-button"
              "::-webkit-outer-spin-button"
              {:height "auto"}]
             ["[type='search']" {:-webkit-appearance "textfield", :outline-offset "-2px"}]
             ["::-webkit-search-decoration" {:-webkit-appearance "none"}]
             ["::-webkit-file-upload-button"
              {:-webkit-appearance "button", :font "inherit"}]
             ["summary" {:display "list-item"}]
             ["blockquote"
              "dl"
              "dd"
              "h1"
              "h2"
              "h3"
              "h4"
              "h5"
              "h6"
              "hr"
              "figure"
              "p"
              "pre"
              {:margin "0"}]
             ["button" {:background-color "transparent", :background-image "none"}]
             ["button:focus"
              {:outline "1px dotted"}
              {:outline "5px auto -webkit-focus-ring-color"}]
             ["fieldset" {:margin "0", :padding "0"}]
             ["ol" "ul" {:list-style "none", :margin "0", :padding "0"}]
             ["html"
              {:font-family
               "ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, \"Noto Sans\", sans-serif, \"Apple Color Emoji\", \"Segoe UI Emoji\", \"Segoe UI Symbol\", \"Noto Color Emoji\"",
               :line-height "1.5"}]
             ["body" {:font-family "inherit", :line-height "inherit"}]
             ["*"
              "*::before"
              "*::after"
              {:border-width "0", :border-style "solid", :border-color "#e5e7eb"}]
             ["hr" {:border-top-width "1px"}]
             ["img" {:border-style "solid"}]
             ["textarea" {:resize "vertical"}]
             ["input::placeholder"
              "textarea::placeholder"
              {:opacity "1", :color "#9ca3af"}]
             ["button" "[role=\"button\"]" {:cursor "pointer"}]
             ["table" {:border-collapse "collapse"}]
             ["h1"
              "h2"
              "h3"
              "h4"
              "h5"
              "h6"
              {:font-size "inherit", :font-weight "inherit"}]
             ["a" {:color "inherit", :text-decoration "inherit"}]
             ["button"
              "input"
              "optgroup"
              "select"
              "textarea"
              {:padding "0", :line-height "inherit", :color "inherit"}]
             ["pre"
              "code"
              "kbd"
              "samp"
              {:font-family
               "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, \"Liberation Mono\", \"Courier New\", monospace"}]
             ["img"
              "svg"
              "video"
              "canvas"
              "audio"
              "iframe"
              "embed"
              "object"
              {:display "block", :vertical-align "middle"}]
             ["img" "video" {:max-width "100%", :height "auto"}]
             [:*
              {:--tw-ring-inset         "var(--tw-empty,/*!*/ /*!*/)",
               :--tw-ring-offset-width  "0px",
               :--tw-ring-offset-color  "#fff",
               :--tw-ring-color         "rgba(59,130,246,0.5)",
               :--tw-ring-offset-shadow "0 0 transparent",
               :--tw-ring-shadow        "0 0 transparent"}]],
            :bases
            {:.bg-green-500
             [:.bg-green-500
              {:--tw-bg-opacity  "1",
               :background-color "rgba(16, 185, 129, var(--tw-bg-opacity))"}],
             :.hover:bg-red-500:hover
             [".hover\\:bg-red-500:hover"
              {:--tw-bg-opacity  "1",
               :background-color "rgba(239, 68, 68, var(--tw-bg-opacity))"}]},
            :bases:sm
            {:.sm:bg-yellow-400
             {:identifier :media,
              :value
              {:media-queries {:min-width "640px", :screen true},
               :rules
               ([[".sm\\:bg-yellow-400"
                  {:--tw-bg-opacity  "1",
                   :background-color "rgba(251, 191, 36, var(--tw-bg-opacity))"}]])}}},
            :bases:md  {},
            :bases:lg  {},
            :bases:xl  {},
            :bases:2xl {},
            :user-css  {}}
           (tcore/result-css-map [:.bg-green-500 :.hover:bg-red-500:hover :.sm:bg-yellow-400])))))

(deftest ->hcss*-restult
  (is (= (frequencies (string->vec (tcore/->hcss*
                                    [:.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus])))
         (frequencies (string->vec "bg-pink-100 hover:bg-pink-400 focus:bg-pink-400"))))
  (is (= (count (string->vec (tcore/->hcss* [:.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus])))
         (count (string->vec "bg-pink-100 hover:bg-pink-400 focus:bg-pink-400"))))
  (is (= (frequencies (string->vec "bg-pink-100 hover:bg-pink-400 focus:bg-pink-400"))
         (frequencies (string->vec (tcore/->hcss* [:.bg-pink-100 :.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus])))))
  (is (not= (frequencies (string->vec (tcore/->hcss* [:.bg-pink-100 :.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus])))
            (frequencies (string->vec "bg-pink-100 hover:bg-pink-400 bg-pink-100 focus:bg-pink-400")))))

(deftest css-keys-in-hiccup-atom
  (testing "Testing css-keys-in-hiccup atom"
    (let [_addingcss-to-atom! (doall
                                [(tcore/->hcss* [:.bg-green-500 :.hover:bg-red-500:hover :.sm:bg-yellow-400])
                                 (tcore/->hcss* [:.bg-red-100 :.hover:bg-red-600:hover])
                                 (tcore/->hcss* [:.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus])
                                 (tcore/->hcss* [:.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus])
                                 (tcore/->hcss* [:.bg-pink-100 :.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus])
                                 (tcore/->hcss* [:.bg-pink-100 :.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus])])]
      (is (= (sort @tprep/css-keys-in-hiccup)
             (sort [:.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus
                    :.bg-green-500 :.hover:bg-red-500:hover :.sm:bg-yellow-400 :.bg-red-100
                    :.hover:bg-red-600:hover])))
      (tcore/->hcss* (sort [:.bg-pink-100 :.bg-purple-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus :.bg-red-100]))
      (is (= (sort @tprep/css-keys-in-hiccup) (sort [:.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus
                                                     :.bg-green-500 :.hover:bg-red-500:hover :.sm:bg-yellow-400 :.bg-red-100
                                                     :.hover:bg-red-600:hover :.bg-purple-100])))
      (is (not= (sort @tprep/css-keys-in-hiccup) (sort [:.bg-pink-100 :.hover:bg-pink-400:hover :.focus:bg-pink-400:focus
                                                        :.bg-green-500 :.hover:bg-red-500:hover :.sm:bg-yellow-400 :.bg-red-100
                                                        :.hover:bg-red-600:hover])))
      (is (= (:bases (tcore/result-css-map @tprep/css-keys-in-hiccup))
             {:.bg-pink-100             [:.bg-pink-100 {:--tw-bg-opacity "1", :background-color "rgba(252, 231, 243, var(--tw-bg-opacity))"}],
              :.hover:bg-pink-400:hover [".hover\\:bg-pink-400:hover" {:--tw-bg-opacity "1", :background-color "rgba(244, 114, 182, var(--tw-bg-opacity))"}],
              :.focus:bg-pink-400:focus [".focus\\:bg-pink-400:focus" {:--tw-bg-opacity "1", :background-color "rgba(244, 114, 182, var(--tw-bg-opacity))"}],
              :.bg-green-500            [:.bg-green-500 {:--tw-bg-opacity "1", :background-color "rgba(16, 185, 129, var(--tw-bg-opacity))"}],
              :.hover:bg-red-500:hover  [".hover\\:bg-red-500:hover" {:--tw-bg-opacity "1", :background-color "rgba(239, 68, 68, var(--tw-bg-opacity))"}],
              :.bg-red-100              [:.bg-red-100 {:--tw-bg-opacity "1", :background-color "rgba(254, 226, 226, var(--tw-bg-opacity))"}],
              :.hover:bg-red-600:hover  [".hover\\:bg-red-600:hover" {:--tw-bg-opacity "1", :background-color "rgba(220, 38, 38, var(--tw-bg-opacity))"}],
              :.bg-purple-100           [:.bg-purple-100 {:--tw-bg-opacity "1", :background-color "rgba(237, 233, 254, var(--tw-bg-opacity))"}]}))
      (is (= (:bases:sm (tcore/result-css-map @tprep/css-keys-in-hiccup))
             {:.sm:bg-yellow-400 #garden.types.CSSAtRule{:identifier :media, :value {:media-queries {:min-width "640px", :screen true}, :rules ([[".sm\\:bg-yellow-400" {:--tw-bg-opacity "1", :background-color "rgba(251, 191, 36, var(--tw-bg-opacity))"}]])}}})))
    (is (empty? (:bases:md (tcore/result-css-map @tprep/css-keys-in-hiccup))))
    (is (= (tcore/garden-to :css)
           "*, *::before, *::after {\n  box-sizing: border-box;\n}\n\n:root {\n  -moz-tab-size: 4;\n  tab-size: 4;\n}\n\nhtml {\n  line-height: 1.15;\n  -webkit-text-size-adjust: 100%;\n}\n\nbody {\n  margin: 0;\n}\n\nbody {\n  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji';;\n}\n\nhr {\n  height: 0;\n  color: inherit;\n}\n\nabbr[title] {\n  -webkit-text-decoration: underline dotted;\n  text-decoration: underline dotted;\n}\n\nb, strong {\n  font-weight: bolder;\n}\n\ncode, kbd, samp, pre {\n  font-family: ui-monospace, SFMono-Regular, Consolas, 'Liberation Mono', Menlo, monospace;\n  font-size: 1em;\n}\n\nsmall {\n  font-size: 80%;\n}\n\nsub, sup {\n  font-size: 75%;\n  line-height: 0;\n  position: relative;\n  vertical-align: baseline;\n}\n\nsub {\n  bottom: -0.25em;\n}\n\nsup {\n  top: -0.5em;\n}\n\ntable {\n  text-indent: 0;\n  border-color: inherit;\n}\n\nbutton, input, optgroup, select, textarea {\n  font-family: inherit;\n  font-size: 100%;\n  line-height: 1.15;\n  margin: 0;\n}\n\nbutton, select {\n  text-transform: none;\n}\n\nbutton, [type='button'], [type='reset'], [type='submit'] {\n  -webkit-appearance: button;\n}\n\n::-moz-focus-inner {\n  border-style: none;\n  padding: 0;\n}\n\n:-moz-focusring {\n  outline: 1px dotted ButtonText;\n}\n\n::-webkit-inner-spin-button, ::-webkit-outer-spin-button {\n  height: auto;\n}\n\n[type='search'] {\n  -webkit-appearance: textfield;\n  outline-offset: -2px;\n}\n\n::-webkit-search-decoration {\n  -webkit-appearance: none;\n}\n\n::-webkit-file-upload-button {\n  -webkit-appearance: button;\n  font: inherit;\n}\n\nsummary {\n  display: list-item;\n}\n\nblockquote, dl, dd, h1, h2, h3, h4, h5, h6, hr, figure, p, pre {\n  margin: 0;\n}\n\nbutton {\n  background-color: transparent;\n  background-image: none;\n}\n\nbutton:focus {\n  outline: 1px dotted;\n  outline: 5px auto -webkit-focus-ring-color;\n}\n\nfieldset {\n  margin: 0;\n  padding: 0;\n}\n\nol, ul {\n  list-style: none;\n  margin: 0;\n  padding: 0;\n}\n\nhtml {\n  font-family: ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, \"Noto Sans\", sans-serif, \"Apple Color Emoji\", \"Segoe UI Emoji\", \"Segoe UI Symbol\", \"Noto Color Emoji\";\n  line-height: 1.5;\n}\n\nbody {\n  font-family: inherit;\n  line-height: inherit;\n}\n\n*, *::before, *::after {\n  border-width: 0;\n  border-style: solid;\n  border-color: #e5e7eb;\n}\n\nhr {\n  border-top-width: 1px;\n}\n\nimg {\n  border-style: solid;\n}\n\ntextarea {\n  resize: vertical;\n}\n\ninput::placeholder, textarea::placeholder {\n  opacity: 1;\n  color: #9ca3af;\n}\n\nbutton, [role=\"button\"] {\n  cursor: pointer;\n}\n\ntable {\n  border-collapse: collapse;\n}\n\nh1, h2, h3, h4, h5, h6 {\n  font-size: inherit;\n  font-weight: inherit;\n}\n\na {\n  color: inherit;\n  text-decoration: inherit;\n}\n\nbutton, input, optgroup, select, textarea {\n  padding: 0;\n  line-height: inherit;\n  color: inherit;\n}\n\npre, code, kbd, samp {\n  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, \"Liberation Mono\", \"Courier New\", monospace;\n}\n\nimg, svg, video, canvas, audio, iframe, embed, object {\n  display: block;\n  vertical-align: middle;\n}\n\nimg, video {\n  max-width: 100%;\n  height: auto;\n}\n\n* {\n  --tw-ring-inset: var(--tw-empty,/*!*/ /*!*/);\n  --tw-ring-offset-width: 0px;\n  --tw-ring-offset-color: #fff;\n  --tw-ring-color: rgba(59,130,246,0.5);\n  --tw-ring-offset-shadow: 0 0 transparent;\n  --tw-ring-shadow: 0 0 transparent;\n}\n\n.hover\\:bg-pink-400:hover {\n  --tw-bg-opacity: 1;\n  background-color: rgba(244, 114, 182, var(--tw-bg-opacity));\n}\n\n.bg-purple-100 {\n  --tw-bg-opacity: 1;\n  background-color: rgba(237, 233, 254, var(--tw-bg-opacity));\n}\n\n.bg-red-100 {\n  --tw-bg-opacity: 1;\n  background-color: rgba(254, 226, 226, var(--tw-bg-opacity));\n}\n\n.hover\\:bg-red-600:hover {\n  --tw-bg-opacity: 1;\n  background-color: rgba(220, 38, 38, var(--tw-bg-opacity));\n}\n\n.bg-green-500 {\n  --tw-bg-opacity: 1;\n  background-color: rgba(16, 185, 129, var(--tw-bg-opacity));\n}\n\n.hover\\:bg-red-500:hover {\n  --tw-bg-opacity: 1;\n  background-color: rgba(239, 68, 68, var(--tw-bg-opacity));\n}\n\n.bg-pink-100 {\n  --tw-bg-opacity: 1;\n  background-color: rgba(252, 231, 243, var(--tw-bg-opacity));\n}\n\n.focus\\:bg-pink-400:focus {\n  --tw-bg-opacity: 1;\n  background-color: rgba(244, 114, 182, var(--tw-bg-opacity));\n}\n\n.container {\n  width: 100%;\n}\n\n@media (min-width: 640px) {\n\n  .container {\n    max-width: 640px;\n  }\n\n}\n\n@media (min-width: 768px) {\n\n  .container {\n    max-width: 768px;\n  }\n\n}\n\n@media (min-width: 1024px) {\n\n  .container {\n    max-width: 1024px;\n  }\n\n}\n\n@media (min-width: 1280px) {\n\n  .container {\n    max-width: 1280px;\n  }\n\n}\n\n@media (min-width: 1536px) {\n\n  .container {\n    max-width: 1536px;\n  }\n\n}\n\n@media screen and (min-width: 640px) {\n\n  \n  \n  .sm\\:bg-yellow-400 {\n    --tw-bg-opacity: 1;\n    background-color: rgba(251, 191, 36, var(--tw-bg-opacity));\n  }\n\n}\n\n@keyframes spin {\n\n  from {\n    transform: rotate(0deg;\n  }\n  \n  to {\n    transform: rotate(360deg;\n  }\n\n}\n\n@keyframes ping {\n\n75%, 100% {\n    transform: scale(2);\n    opacity: 0;\n  }\n\n}\n\n@keyframes pulse {\n\n0%, 100% {\n    opacity: 1;\n  }\n  \n50% {\n    opacity: .5;\n  }\n\n}\n\n@keyframes bounce {\n\n0%, 100% {\n    transform: translateY(-25%);\n    animationTimingFunction: cubic-bezier(0.8, 0, 1, 1);\n  }\n  \n50% {\n    transform: translateY(0);\n    animationTimingFunction: cubic-bezier(0, 0, 0.2, 1);\n  }\n\n}"))))

(deftest user-defined-classes
  (let [class1 (tcore/->css* [[:.class-1 {:width "100px"}]])
        class2 (tcore/->css* [[:.class-2 {:width "200px"}]])]
    (testing "user defined classes"
      (is (not (empty? @tprep/css-keys-in-hiccup)))
      (is (not (empty? @tprep/user-css)))
      (is (= 2 (count @tprep/css-keys-in-hiccup))))))
