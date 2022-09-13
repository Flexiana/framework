<img src="resources/images/Xiana.png" width="242">

# Tutorials

- [App startup](#app-startup)
- [Configuration](#configuration)
- [Dependencies](#dependencies)
- [Database migration](#database-migration)
- [Interceptors typical use-case, and ordering](#interceptors-typical-use-case-and-ordering)
- [Defining new interceptors](#defining-new-interceptors)
    - [Interceptor example](#interceptor-example)
- [Router and controller interceptors](#router-and-controller-interceptors)
- [Providing default interceptors](#providing-default-interceptors)
- [Interceptor overriding](#interceptor-overriding)
- [Routes](#routes)
- [Action](#action)
- [Database-access](#database-access)
- [View](#view)
- [Side-effects](#side-effects)
- [Session management](#session-management)
- [Role based access and data ownership control](#role-based-access-and-data-ownership-control)
- [WebSockets](#websockets)
    - [WebSockets routing](#websockets-routing)
    - [Route matching](#route-matching)
- [Server-Sent Events (SSE)](#server-sent-events-sse)
- [Scheduler](#scheduler)

## App startup

Starting up an application takes several well-defined steps:

- Reading the configuration
- setting up dependencies
- spinning up a web-server

## Configuration

Apps built with Xiana are configurable in several ways. It uses [yogthos/config](https://github.com/yogthos/config) to
resolve basic configuration from `config.edn`, `.lein-env`, `.boot-env` files, environment variables and system
properties. Additionally

- Xiana looks for an `edn` file pointed with `:xiana-config` variable for overrides
- You can define a value to read from any other (already defined) key, and pass a default value.

In practice this means you can define a config value like this:

```clojure
:xiana/test {:test-value-1 "$property"
             :test-value-2 "$foo | baz"}
```

and this will be resolved as

```clojure
:xiana/test {:test-value-1 "the value of 'property' key, or nil"
             :test-value-2 "the value of `foo` key or \"baz\""}
```

The value of property key can come from the config files, environment variables, or from system properties.

## Dependencies

Database connection, external APIs or session storage, the route definition, setting up scheduled executors or
doing migrations are our dependencies. These dependencies should be reachable via the passed around state. To achieve
this, it should be part of the `:deps` map in the state. Any other configuration what you need in runtime should be part
of this map too.

The system configuration and start-up with the chainable set-up:

```clojure
(defn ->system
  [app-cfg]
  (-> (config/config app-cfg)                    ;Read config
      routes/reset                               ;set up routing
      db/start                                   ;set up database connection
      db/migrate!                                ;running migrations
      session/init-backend                       ;initialize session storage
      (scheduler/start actions/ping 10000)       ;starting a scheduler
      ws/start))                                 ;spinning up the webserver

(def app-cfg
  {:routes                  routes                              ;injecting route definition
   :router-interceptors     []                                  ;definition of router interceptors
   :controller-interceptors [(xiana-interceptors/muuntaja)      ;definition of controller interceptors
                             cookies/interceptor
                             xiana-interceptors/params
                             session/interceptor
                             xiana-interceptors/view
                             xiana-interceptors/side-effect
                             db/db-access]})

(defn -main
  [& _args]
  (->system app-cfg))                                           ;starting the application
```

## Database migration

Xiana uses a forked and decorated version of migratus, to extend its functionality with profile based migrations, and
merging migrations from different folders.
You are able to configure your migrations like this for test profile:

```clojure
:xiana/migration {:store         :database
                  :migration-dir ["migrations/common"
                                  "migrations/test_seeds"]}
```

You can run `lein migrate` with migratus parameters like: `create`, `destroy`, `up`, `down`, `init`, `reset`, `migrate`
, `rollback`. It will do the same as migratus, except one more thing: you can use `with profile` lein parameter to
define settings migratus should use. So instead of having only one migration folder you can define one for each of your
profiles.

```shell
lein with-profile +test migrate create default-users
```

Will create `up` and `down` SQL files in folder configured in `config/test/config.edn`, and

```shell
lein with-profile +test migrate migrate
```

will use it.

But without profile:

```shell
lein migrate migrate
```

migratus will use the migrations from a folder, what is configured in `config/dev/config.edn`.

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

## Router and controller interceptors

The router and controller interceptors are executed in the exact same order (enter functions in order, leave functions
in reversed order), but not in the same place of the execution flow. Router interceptors are executed around Xiana's
router, controller interceptors executed around the defined action.

1. router interceptors :enter functions in order
2. router interceptors :leave functions in reversed order
3. routing, and matching
4. controller interceptors :enter functions in order
5. action
6. controller interceptors :leave functions in reversed order

In router interceptors, you are able to interfere with the routing mechanism. Controller interceptors can be interfered
with via route definition. There is an option to define interceptors around creating WebSocket channels, these
interceptors are executed around the `:ws-action` instead of `:action`.

## Providing default interceptors

```clojure
{:router-interceptors     [...]
 :controller-interceptors [...]
 :web-socket-interceptors [...]}
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

## Routes

Route definition is done via [reitit's routing](https://github.com/metosin/reitit) library. Route processing is done
with `xiana.route` namespace. At route definition you can define.

- The [action](#action) that should be executed
- [Interceptor overriding](#interceptor-overriding)
- The required permission for [rbac](#role-based-access-and-data-ownership-control)
- [WebSockets](#websockets) action definition

If any extra parameter is provided here, it's injected into

```clojure
(-> state :request-data :match)
```

in routing step.

Example route definition:

```clojure
["/api" {}
 ["/login" {:post {:action       #'user-controllers/login                     ;Login controller
                   :interceptors {:except [session/interceptor]}}}]           ;the user doesn't have a valid session yet
 ["/posts" {:get    {:action     #'posts-controllers/fetch                    ;controller definition for fetching posts
                     :permission :posts/read}                                 ;set up the permission for fetching posts
            :put    {:action     #'posts-controllers/add
                     :permission :posts/create}
            :post   {:action     #'posts-controllers/update-post
                     :permission :posts/update}
            :delete {:action     #'posts-controllers/delete-post
                     :permission :posts/delete}}]
 ["/notifications" {:get {:ws-action  #'websockets/notifications               ;websocket controller for sending notifications
                          :action     #'notifications/fetch                    ;REST endpoint for fetching notifications
                          :permission :notifications/read}}]                   ;inject permission
 ["/style" {:get {:action       #'style/fetch
                  :organization :who}}]]                                       ;this is not a usual key, the value will go to 
;(-> state :request-data :match :organization) 
;at the routing process 
```

## Action

The action function (controller) in a
single [CRUD application](https://en.wikipedia.org/wiki/Create,_read,_update_and_delete#RESTful_APIs) is for defining
a [view](#view), a [database-query](#database-access) (model) and optionally a [side-effect](#side-effects) function
which will be executed in the following interceptor steps.

```clojure
(defn action
  [state]
  (assoc state :view view/success
               :side-effect behaviour/update-sessions-and-db!
               :query model/fetch-query))
```

## Database access

The `xiana.db/db-access` executes queries from `:query` (for single, non-transactional database access)
and `:db-queries` in this order against the datasource extracted from state. The result will be available
in `(-> state :response-data :db-data)` which is always a sequence.

`db-queries` is still a map, contains `:queries` and `:transaction?` keys. If `:transaction?` is set to `true`,
all `queries` in `db-queries` will be executed in one transaction.

The query should be in [honey SQL](https://github.com/nilenso/honeysql-postgres) format, it will be sql-formatted on
execution:

```clojure
(-> (select :*)
    (from :users)
    (where [:and
            :is_active
            [:or
             [:= :email login]
             [:= :username login]]]))
```

is equal to

```clojure
{:select [:*]
 :from   [:users]
 :where  [:and
          :is-active
          [:or
           [:= :email login]
           [:= :user-name login]]]}
```

Both examples above are leads to

```postgres-sql
["SELECT * FROM users WHERE is_active AND (email = ? OR user_name = ?)" login login] 
```

## View

A view is a function to prepare the final response and saving it into the state based on whatever happened before.

```clojure
(defn success
  [state]
  (let [{:users/keys [id]} (-> state :response-data :db-data first)]
    (assoc state :response {:status  200
                            :body    {:view-type "login"
                                      :data      {:login   "succeed"
                                                  :user-id id}}})))
```

## Side-effects

Conventionally, side-effects interceptor is placed after [action](#action) and [database-access](#database-access),
just
right before [view](#view). At this point, we already have the result of database execution, so we are able to do some
extra refinements, like sending notifications, updating the application state, filtering or mapping the result and so
on.

Adding to the previous examples:

```clojure
(defn update-sessions-and-db!
  "Creates and adds a new session to the server's store for the user that wants to sign-in.
   Avoids duplication by firstly removing the session that is related to this user (if it exists).
   After the session addition, it updates the user's last-login value in the database."
  [state]
  (if (valid-credentials? state)
    (let [new-session-id (str (UUID/randomUUID))
          session-backend (-> state :deps :session-backend)
          user (-> state :response-data :db-data first)]
      (xiana-sessions/add! session-backend new-session-id user)
      (assoc-in state [:response :headers "Session-id"] new-session-id))
    (assoc state :response {:status 401
                            :body   "Unauthorized"})))
```

## Session management

Session management has two mayor components

- session backend
- session interceptors

The session backend can be in-memory or persisted using a json storage in postgres database.

The session interceptors interchanges session data between the session-backend and the app state.

The `xiana.session/interceptor` throws an exception when no valid session-id can be found in the headers, cookies or as
query parameter.

The `xiana.session/guest-session-interceptor` creates a guest session if the session-id is missing, or invalid, which
means:

```clojure
{:session-id (UUID/randomUUID)
 :users/role :guest
 :users/id   (UUID/randomUUID)}
```

will be injected to the session data.

Both interceptors fetching already stored session data into the state at `:enter`, and on `:leave` updates session
storage with the data from `(-> state :session-data)`

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

## WebSockets

To use an endpoint to serve a WebSockets connection, you can define it on route-definition alongside the restfull
action:

```clojure
(def routes
  [[...]
   ["/ws" {:ws-action websocket/echo
           :action    restfull/hello}]])
```

In `:ws-action` function you can provide the reactive functions in `(-> state :response-data :channel)`

```clojure
(:require
  ...
  [xiana.websockets :refer [router string->]]
  ...)

(defonce channels (atom {}))

(def routing
  (partial router routes string->))

(defn chat-action
  [state]
  (assoc-in state [:response-data :channel]
            {:on-receive (fn [ch msg]
                           (routing (update state :request-data
                                            merge {:ch         ch
                                                   :income-msg msg
                                                   :fallback   views/fallback
                                                   :channels   channels})))
             :on-open    (fn [ch]
                           (routing (update state :request-data
                                            merge {:ch         ch
                                                   :channels   channels
                                                   :income-msg "/welcome"})))
             :on-ping    (fn [ch data])
             :on-close   (fn [ch status] (swap! channels dissoc ch))
             :init       (fn [ch])}))
```

The creation of the actual channel happens in Xiana's [handler](conventions.md#handler). All provided reactive functions
have the entire [state](conventions.md#state) to work with.

### WebSockets routing

`xiana.websockets` offers a router function, which supports Xiana concepts. You can define a reitit route and use it
inside WebSockets reactive functions. With Xiana [state](conventions.md#state)
and support of [interceptors](conventions.md#interceptors), with [interceptor override](#interceptor-overriding). You
can define a [fallback function](#websockets), to handle missing actions.

```clojure
(def routes
  (r/router [["/login" {:action       behave/login
                        :interceptors {:inside [interceptors/side-effect
                                                interceptors/db-access]}
                        :hide         true}]]              ;; xiana.websockets/router will not log the message 
            {:data {:default-interceptors [(interceptors/message "Incoming message...")]}}))
```

### Route matching

For route matching Xiana provides a couple of modes:

- extract from string

  The first word of given message as actionable symbol

- from JSON

  The given message parsed as JSON, and `:action` is the actionable symbol

- from EDN

  The given message parsed as EDN, and `:action` is the actionable symbol

- Probe

  It tries to decode the message as JSON, EDN or string in corresponding order.

You can also define your own matching, and use it as a parameter to `xiana.websockets/router`

## Server-Sent Events (SSE)

Xiana contains a simple SSE solution over WebSockets protocol.

Initialization is done by calling `xiana.sse/init`. Clients can subscribe by a route
with `xiana.sse/sse-action` as `:ws-action`. Messages are sent with `xiana.sse/put!` function.

```clojure
(def routes
  [["/sse" {:action sse/sse-action}]
   ["/broadcast" {:action (fn [state]
                            (sse/put! state {:message "This is not a drill!"})
                            state)}]])

(defn ->system
  [app-cfg]
  (-> (config/config app-cfg)
      (route/reset routes)
      sse/init
      ws/start))

(defn -main
  [& _args]
  (->system {}))
```

## Scheduler

To repeatedly execute a function, you can use the `xiana.scheduler/start` function. Below is an implementation of SSE
ping:

```clojure
(ns app.core
  (:require
    [xiana.scheduler :as scheduler]
    [clojure.core.async :as async]))

(defn ping [deps]
  (let [channel (get-in deps [:events-channel :channel])]
    (async/>!! channel {:type      :ping
                        :id        (str (UUID/randomUUID))
                        :timestamp (.getTime (Date.))})))

(defn ->system
  [app-cfg]
  (-> (config/config)
      (merge app-cfg)
      ...
      sse/init
      (scheduler/start ping 10000)
      ...))
```
