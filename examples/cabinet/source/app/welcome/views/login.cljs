(ns app.welcome.views.login
  (:require [app.common.components.button :as button]
            [app.common.components.input :as input]))

(defn- hero
  [form]
  [:section {:class "grid grid-cols-2"}
   [:div {:class "flex flex-col justify-center w-full h-screen bg-red-500"}
    [:h1 {:class "text-center text-white text-8xl font-mono"}
     "Sing In View"]]
   [:div {:class "flex justify-center items-center m-2"}
    form]])

(defn view
  []
  (hero
   [:form
    (-> input/component
        ((input/set-placeholder "Email or username"))
        ((input/render {})))
    (-> input/component
        ((input/set-placeholder "Password"))
        ((input/set-type "password"))
        ((input/render {})))
    (-> button/component
        ((button/set-label "Sign In"))
        ((button/render {:on-click #(js/console.log "Hi from console!")})))]))
