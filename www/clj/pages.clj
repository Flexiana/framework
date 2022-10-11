(ns pages)

(def index
  [:html
   [:head
    [:script {:src "https://unpkg.com/htmx.org@1.6.1"}]]
   [:form {:hx-post "/click" :hx-swap "outerHTML"}
    [:button {:type "submit"} "Don't click this button"]]])

