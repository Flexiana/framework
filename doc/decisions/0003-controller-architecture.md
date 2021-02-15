# 3. Controller basic architecture

Date: 2021-01-27

## Staus

Proposed

## Context
 1. The Controller is responsible for the lifecycle of the framework. It reflects the starting point and the means of controll of all the needed actions that the framework must do (load deps, render views, generate routes etc).
 2. The framework requires a simple and easy controll to the end user.
 3. In turn the interaction of the user with the controller should be simple understandable and destructured in smaller steps that would provide a better grain of detail in the controll of the application, validation and error management etc.
 4. The framework rational requires a functional approach but in a way that would be easy for new users and in a way that it would use user's previous concept knowledge from similar frameworks in other programming languages.
 
## Decisions
### 1. Controller being `monadic`
#### Explanation:
The Controller would be a composition of different functions in steps as was created in the initial draft by Lukáš Rychtecký.
#### Why:
This would provide the user with the ability to controll each
step on it's creation.
##### Example:

``` clojure
(defn controller
  []
  (-> {}
      (set-routes)
      (set-template)
      (set-response )
      (set-middlewares )
      (set-actions )
      (set-deps)
      (set-session))
  )
```
### 2. Controller use a State map.
#### Explanation:
The Controller would be 'feeded' with an initial state-map that the user would provide. The state-map would hold the data that would be needed to build the Controller. This approach is similar to the State monad but it would return only the result of the configuration and discard the initial state.
#### Why:
This helps in two ways:
1. We can enforce a specific stucture in the definition of the state-map to the user. This would give a clearer understanding on the 'ingredients' that a controller would need to created to the user, as it would be a map which its keys would represent the different stages of the controller.
2. We would be able to validate the state-map and its data.
##### Example:
###### state map:
``` clojure
(def state {:http-request {:method :get
                           :url "/hi"
                           :request {:action :select :params {}}}
            :response {:headers (list {"Content-Type" "txt/html"}
                                 {"charset" "UTF-8"})
                       :body nil}
            :request-data {:model Foo
                           :template (comp temp)
                           :middlewares []}
            :session-data {}
            :deps {}})
```

###### state map schema:
``` clojure
;; mali schema for the State Map
(def State-map
  [:map
   [:http-request [:map
                  [:method [:enum :get :post :put]]
                  [:url [:vector]]
                  [:request [:action [:enum :select :update :delet :insert]]]]]
   [:response [:map
               [:headers [:vector]]
               [:body [:or [:string] [:vector] [:nil]]]
               ]]
   [:request-data [:map
                   [:model ;;malli schema
                    [:vector]]
                   [:template ;; hiccup or static
                    [:or [:vector]
                     [:string]]]
                   [:middlewares ;; list of functions to used for the middleware
                    [:vector]]]]
   [:deps [:map]]
   [:session [:map]]])
```

###### controller with state-map:

``` clojure
(defn controller
  [state-map]
  (-> {}
      (set-routes state-map)
      (set-template state-map)
      (set-response state-map)
      (set-middlewares state-map)
      (set-actions state-map)
      (set-deps state-map)
      (set-session state-map))
  )
```

### Using a metosin/reitit wrapper
#### Explanation:
The controller would be wraped in a thin reitit wrapper with initial approach.
#### Why:
1. Elevates the use of reitit and its already implemented functionality and safeguards.
2. Availability to perform validation on map to gurantee the correct definition of each stage.
##### Exapmle:
###### controller result map

``` clojure
{:route ["/hi" :get],
 :template #function[poc.core/temp],
 :response
 {:headers ({"Content-Type" "txt/html"} {"charset" "UTF-8"}), :body nil},
 :middlewares [],
 :actions {:action :select, :params {}},
 :deps {},
 :session nil}
```
###### result map schema

``` clojure
;; internal schema of an instance of a state-map
(def instance-map
  [:map
   [:route [:vector]]
   [:respones [:map [:headers [:list]
                     :body [:or [:string] [:vector]]]]]
   [:template [:or [:string] [:vector]]]
   [:actions [:vector [:and [:enum :select :delete :update :insert] [:map]]]]
   [:deps [:map]]
   [:middlewares [:list]]
   [:session [:map]]])
;;
```
###### controller build and routes

``` clojure
(defn build-controller
  [ctrl]
  )

(defn generate-routes
  [ctrl]
  (ring/ring-handler
   (ring/router
    ["/api"
     (:route ctrl)]
    {:data {:coercion reitit.coercion.spec/coercion
              :middleware [rrc/coerce-exceptions-middleware
                           rrc/coerce-request-middleware
                           rrc/coerce-response-middleware]}})))
```
