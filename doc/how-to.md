<img src="resources/images/Xiana.png" width="242">

# How to

- [Defining new interceptors](#defining-new-interceptors)
    - [Interceptor example](#interceptor-example)
- [Providing default interceptors](#providing-default-interceptors)
- [Interceptor overriding](#interceptor-overriding)
- [Role based access and data ownership control](#role-based-access-and-data-ownership-control)
- [Access and data ownership control](#access-and-data-ownership-control)
    - [Role set definition](#role-set-definition)
    - [Provide resource/action at routing](#provide-resourceaction-at-routing)
    - [Application start-up](#application-start-up)
    - [Access control](#access-control)
    - [Data ownership](#data-ownership)
- [Login implementation](#login-implementation)
- [Logout implementation](#logout-implementation)

## Defining new interceptors

The interceptor is a map, can have three functions like:

`:enter` Runs while we are going down from the request to it's action, in the order of executors

`:leave` Runs while we're going up from the action to the response.

`:error` Executed when any error thrown while executing the two other functions

and a `:name` can be defined. All keys are optional, and if it missing it's replaced by `identity`.

The provided functions are should have one parameter, the application state, and should return the modified state.

### Interceptor example

```clojure

{:name  :sample-interceptor
 :enter (fn [state]
          (println "Enter: " state)
          (-> state
              (transform-somehow)
              (or-do-side-effects)))
 :leave (fn [state]
          (println "Leave: " state)
          state)
 :error (fn [state]
          (println "Error: " state)
          ;; Here `state` should have previously thrown exception
          ;; stored in `:error` key.
          ;; you can do something useful with it (e.g. log it)
          ;; and/or handle it by `dissoc`ing from the state.
          ;; In that case remaining `leave` interceptors will be executed.
          (assoc state :response {:status 500 :body "Error occurred while printing out state"}))}
```

## Providing default interceptors

The router and controller interceptors definition is part of the application startup. The system's dependency map should
contain two sequence of interceptors like

```clojure
(def app-cfg
  {:routes                  routes
   :router-interceptors     [...]
   :controller-interceptors [...]
   :web-socket-interceptors [...]})
```

## Interceptor overriding

On route definition you can interfere with the default controller interceptors. With the route definition you are able
to set up different controller interceptors other than the ones already defined with the app. There are three ways to do
it:

```clojure
... {:action       #(do something)
     :interceptors [...]}
```

will override all controller interceptors

```clojure
... {:action       #(do something)
     :interceptors {:around [...]}}
```

will extend the defaults around

```clojure
... {:action       #(do something)
     :interceptors {:inside [...]}}
```

will extend the defaults inside

```clojure
... {:action       #(do something)
     :interceptors {:inside [...]
                    :around [...]}}
```

will extend the defaults inside and around

```clojure
... {:action       #(do something)
     :interceptors {:except [...]}}
```

will skip the excepted interceptors from defaults

## Role based access and data ownership control

To get the benefits of [tiny RBAC](https://github.com/Flexiana/tiny-rbac) library you need to provide the resource and
the action for your endpoint in [router](#routes) definition:

```clojure
[["/api"
  ["/image" {:delete {:action     delete-action
                      :permission :image/delete}}]]]
```

and add your role-set into your app's [dependencies](#dependencies-and-configuration):

```clojure
(defn ->system
  [app-cfg]
  (-> (config/config app-cfg)
      xiana.rbac/init
      ws/start))
```

On `:enter`, the interceptor performs the permission check. It determines if the action allowed for the user found
in `(-> state :session-data :user)`. If access to the resource/action isn't permitted, then the response is:

```clojure
{:status 403
 :body   "Forbidden"}
```

If a permission is found, then it goes into `(-> state :request-data :user-permissions)` as a parameter for data
ownership processing.

On `:leave`, executes the restriction function found in `(-> state :request-data :restriction-fn)`. The `restriction-fn`
should look like this:

```clojure
(defn restriction-fn
  [state]
  (let [user-permissions (get-in state [:request-data :user-permissions])]
    (cond
      (user-permissions :image/all) state
      (user-permissions :image/own) (let [session-id (get-in state [:request :headers "session-id"])
                                          session-backend (-> state :deps :session-backend)
                                          user-id (:users/id (session/fetch session-backend session-id))]
                                      (update state :query sql/merge-where [:= :owner.id user-id])))))
```

The rbac interceptor must be placed between the [action](#action) and the [db-access](#database-access) interceptors in
the interceptor chain.

## Access and data ownership control

[RBAC](./tutorials.md#role-based-access-and-data-ownership-control) is a handy way to restrict user actions on different
resources. It's a role-based access control and helps you to implement data ownership control. The `rbac/interceptor`
should be placed [inside](./tutorials.md#interceptor-overriding) [db-access](./interceptors.md#db-access).

### Role set definition

For [tiny-RBAC](https://github.com/Flexiana/tiny-rbac) you should provide
a [role-set](https://github.com/Flexiana/tiny-rbac#builder). It's a map which defines the application resources, the
actions on it, the roles with the different granted actions, and restrictions for data ownership control. This map must
be placed in [deps](./conventions.md#dependencies).

Here's an example role-set for an image service:

```clojure
(def role-set
  (-> (b/add-resource {} :image)
      (b/add-action :image [:upload :download :delete])
      (b/add-role :guest)
      (b/add-inheritance :member :guest)
      (b/add-permission :guest :image :download :all)
      (b/add-permission :member :image :upload :all)
      (b/add-permission :member :image :delete :own)))
```

It defines a role-set with:

- an `:image` resource,
- `:upload :download :delete` actions on `:image` resource
- a `:guest` role, who can download all the images
- a `:member` role, who inherits all of `:guest`'s roles, can upload `:all` images, and delete `:own` images.

### Provide resource/action at routing

The resource and action can be defined on route definition. The RBAC interceptor will check permissions against what is
defined here:

```clojure
(def routes
  [["/api" {:handler handler-fn}
    ["/image" {:get    {:action     get-image
                        :permission :image/download}
               :put    {:action     add-image
                        :permission :image/upload}
               :delete {:action     delete-image
                        :permission :image/delete}}]]])
```

### Application start-up

```clojure
(def role-set
  (-> (b/add-resource {} :image)
      (b/add-action :image [:upload :download :delete])
      (b/add-role :guest)
      (b/add-inheritance :member :guest)
      (b/add-permission :guest :image :download :all)
      (b/add-permission :member :image :upload :all)
      (b/add-permission :member :image :delete :own)))

(def routes
  [["/api" {:handler handler-fn}
    ["/login" {:action       login
               :interceptors {:except [session/interceptor]}}]
    ["/image" {:get    {:action     get-image
                        :permission :image/download}
               :put    {:action     add-image
                        :permission :image/upload}
               :delete {:action     delete-image
                        :permission :image/delete}}]]])

(defn ->system
  [app-cfg]
  (-> (config/config)
      (merge app-cfg)
      session-backend/init-backend
      routes/reset
      db-core/connect
      db-core/migrate!
      ws/start))

(def app-cfg
  {:routes                  routes
   :role-set                role-set
   :controller-interceptors [interceptors/params
                             session/interceptor
                             rbac/interceptor
                             interceptors/db-access]})

(defn -main
  [& _args]
  (->system app-cfg))

```

### Access control

Prerequisites:

- role-set in `(-> state :deps :role-set)`
- route definition has `:permission` key
- user's role is in `(-> state :session-data :users/role)`

If the `:permission` key is missing, all requests are going to be **granted**. If `role-set` or `:users/role` is
missing, all requests are going to be **denied**.

When `rbac/interceptor` `:enter` is executed, it checks if the user has any permission on the
pre-defined `resource/action` pair. If there is any, it collects all of them (including inherited permissions) into a
set of format: `:resource/restriction`.

For example:

```clojure
:image/own
```

means the given user is granted the permission to do the given `action` on `:own` `:image` resource. This will help you
to implement [data ownership](#data-ownership) functions. This set is associated
in `(-> state :request-data :user-permissions)`

If user cannot perform the given action on the given resource (neither by inheritance nor by direct permission), the
interceptor will interrupt the execution flow with the response:

```clojure
{:status 403
 :body   "Forbidden"}
```

### Data ownership

Data ownership control is about restricting database results only to the elements on which the user is able to perform
the given action. In the context of the example above, it means `:member`s are able to delete only the owned `:image`s.
At this point, you can use the result of the [access control](#access-control) from the state. Continuing with the same
example.

From this generic query

```clojure
{:delete [:*]
 :from   [:images]
 :where  [:= :id (get-in state [:params :image-id])]}
```

you want to switch to something like this:

```clojure
{:delete [:*]
 :from   [:images]
 :where  [:and
          [:= :id (get-in state [:params :image-id])]
          [:= :owner.id user-id]]}
```

To achieve this, you can simply provide a `restriction function` into `(-> state :request-data :restriction-fn)`
The `user-permissions` is a set, so it can be easily used for making conditions:

```clojure
(defn restriction-fn
  [state]
  (let [user-permissions (get-in state [:request-data :user-permissions])]
    (cond
      (user-permissions :image/own) (let [user-id (get-in state [:session-data :users/id])]
                                      (update state :query sql/merge-where [:= :owner.id user-id]))
      :else state)))
```

And finally, the only missing piece of code: the model, and the action

```clojure

(defn delete-query
  [state]
  {:delete [:*]
   :from   [:images]
   :where  [:= :id (get-in state [:params :image-id])]})

(defn delete-image
  [state]
  (-> state
      (assoc :query (delete-query state))
      (assoc-in [:request-data :restriction-fn] restriction-fn)))
```

## Login implementation

Xiana framework does not have any login or logout functions, as every application has its own user management logic.
Though Xiana offers all the tools to easily implement them. One of the default interceptors is the session interceptor.
If included, it can validate a request only if the session already exists in session storage. To log in a user, simply
add its session data to the storage. (TODO: where? What is the exact key to modify?). All sessions should have a unique
UUID as session-id. The active session lives under `(-> state :session-data)`. On every request, before reaching the
action defined by the route, the interceptor checks `[:headers :session-id]` among other things. Which is the id of the
current session. The session is then loaded in session storage. If the id is not found, the execution flow is
interrupted with the response:

```clojure
{:status 401
 :body   "Invalid or missing session"}
```

To implement login, you need to [use the session interceptor](tutorials.md#interceptor-overriding) in

```clojure
(let [;; Create a unique ID
      session-id (UUID/randomUUID)]
  ;; Store a new session in session storage
  (add! session-storage session-id {:session-id session-id})
  ;; Make sure session-id is part of the response
  (assoc-in state [:response :headers :session-id] (str session-id)))
```

or use the `guest-session` interceptor, which creates a guest session for unknown, or missing sessions.

For role-based access control, you need to store the actual user in your session data. First, you'll have to query it
from the database. It is best placed in models/user namespace. Here's an example:

```clojure
(defn fetch-query
  [state]
  (let [login (-> state :request :body-params :login)]
    (-> (select :*)
        (from :users)
        (where [:and
                :is_active
                [:or
                 [:= :email login]
                 [:= :username login]]]))))
```

To execute it, place `db-access` interceptor in the interceptors list. It injects the query result into the state. If
you already have this injected, you can modify your create session function like this:

```clojure
(let [;; Get user from database result
      user (-> state :response-data :db-data first)
      ;; Create session
      session-id (UUID/randomUUID)]
  ;; Store the new session in session storage. Notice the addition of user. 
  (add! session-storage session-id (assoc user :session-id session-id))
  ;; Make sure session-id is part of the response
  (assoc-in state [:response :headers :session-id] (str session-id)))
```

Be sure to remove user's password and any other sensitive information before storing it:

```clojure
(let [;; Get user from database result
      user (-> state
               :response-data
               :db-data
               first
               ;; Remove password for session storage
               (dissoc :users/password))
      ;; Create session id
      session-id (UUID/randomUUID)]
  ;; Store the new session in session storage
  (add! session-storage session-id (assoc user :session-id session-id))
  ;; Make sure session-id is part of the response
  (assoc-in state [:response :headers :session-id] (str session-id)))
```

Next, we check if the credentials are correct, so we use an `if` statement.

```clojure
(if (valid-credentials?)
  (let [;; Get user from database result
        user (-> state
                 :response-data
                 :db-data
                 first
                 ;; Remove password for session storage
                 (dissoc :users/password))
        ;; Create session ID
        session-id (UUID/randomUUID)]
    ;; Store the new session in session storage
    (add! session-storage session-id (assoc user :session-id session-id))
    ;; Make sure session-id is part of the response
    (assoc-in state [:response :headers :session-id] (str session-id)))
  (throw (ex-info "Missing session data"
                  {:xiana/response
                   {:body   "Login failed"
                    :status 401}})))
```

Xiana provides `xiana.hash` to check user credentials:

```clojure
(defn- valid-credentials?
  "It checks that the password provided by the user matches the encrypted password from the database."
  [state]
  (let [user-provided-pass (-> state :request :body-params :password)
        db-stored-pass (-> state :response-data :db-data first :users/password)]
    (and user-provided-pass
         db-stored-pass
         (hash/check state user-provided-pass db-stored-pass))))
```

The login logic is done, but where to place it?

Do you remember the [side effect interceptor](./interceptors.md#side-effect)? It's running after we have the query
result
from the database, and before the final response is rendered with the [view interceptor](./interceptors.md#view). The
place for the function defined above is in the interceptor chain. How does it go there? Let's see
an [action](./conventions.md#action)

```clojure
(defn action
  [state]
  (assoc state :side-effect side-effects/login))
```

This is the place for injecting the database query, too:

```clojure
(defn action
  [state]
  (assoc state :side-effect side-effects/login
               :query model/fetch-query))
```

But some tiny thing is still missing. The definition of the response in the all-ok case. A happy path response.

```clojure
(defn login-success
  [state]
  (let [id (-> state :response-data :db-data first :users/id)]
    (-> state
        (assoc-in [:response :body]
                  {:view-type "login"
                   :data      {:login   "succeed"
                               :user-id id}})
        (assoc-in [:response :status] 200))))
```

And finally the [view](tutorials.md#view) is injected in the action function:

```clojure
(defn action
  [state]
  (assoc state :side-effect side-effects/login
               :view view/login-success
               :query model/fetch-query))
```

## Logout implementation

To do a logout is much easier than a login implementation. The `session-interceptor` does half of the work, and if you
have a running session, then it will not complain. The only thing you should do is to remove the actual session from
the `state`
and from session storage. Something like this:

```clojure
(defn logout
  [state]
  (let [session-store (get-in state [:deps :session-backend])
        session-id (get-in state [:session-data :session-id])]
    (session/delete! session-store session-id)
    (dissoc state :session-data)))
```

Add the `ok` response

```clojure
(defn logout-view
  [state]
  (-> state
      (assoc-in [:response :body]
                {:view-type "logout"
                 :data      {:logout "succeed"}})
      (assoc-in [:response :status] 200)))
```

and use it:

```clojure
(defn logout
  [state]
  (let [session-store (get-in state [:deps :session-backend])
        session-id (get-in state [:session-data :session-id])]
    (session/delete! session-store session-id)
    (-> state
        (dissoc :session-data)
        (assoc :view views/logout-view))))
```
