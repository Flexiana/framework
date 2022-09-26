<img src="resources/images/Xiana.png" width="242">

# Tutorials

- [Application startup](#application-startup)
- [Configuration](#configuration)
- [Dependencies](#dependencies)
- [Defining new interceptors](#defining-new-interceptors)
    - [Interceptor example](#interceptor-example)
- [Router and controller interceptors](#router-and-controller-interceptors)
- [Providing default interceptors](#providing-default-interceptors)
- [Interceptor overriding](#interceptor-overriding)
- [Routes](#routes)
- [Action](#action)
- [Database migration](#database-migration)
- [Database-access](#database-access)
- [View](#view)
- [Side-effects](#side-effects)
- [Session management](#session-management)
    - [In memory backend](#in-memory-session-backend)
    - [Persistent backend](#persistent-session-backend)
    - [Session interceptors](#session-interceptors)
- [Role based access and data ownership control](#role-based-access-and-data-ownership-control)
- [WebSockets](#websockets)
    - [WebSockets routing](#websockets-routing)
    - [Route matching](#route-matching)
- [Server-Sent Events (SSE)](#server-sent-events-sse)
- [Scheduler](#scheduler)
- [Login implementation](#login-implementation)
- [Logout implementation](#logout-implementation)
- [Access and data ownership control](#access-and-data-ownership-control)
    - [Role set definition](#role-set-definition)
    - [Provide resource/action at routing](#provide-resourceaction-at-routing)
    - [Application start-up](#application-start-up)
    - [Access control](#access-control)
    - [Data ownership](#data-ownership)

## Application startup

Starting up an application takes several well-defined steps:

- reading the configuration
- setting up dependencies
- spinning up a web-server

## Configuration

Apps built with Xiana are configurable in several ways. It uses [yogthos/config](https://github.com/yogthos/config) to
resolve basic configuration from `config.edn`, `.lein-env`, `.boot-env` files, environment variables and system
properties. Additionally

- Xiana looks for an `.edn` file pointed with `:xiana-config` variable for overrides
- You can define a key to read from any other (already defined) value, and/or pass a default value.

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

## Database migration

Database migration is based on the following principles:

1. The migration process is based on a stack of immutable changes. If at some point you want to change the schema or the
   content of the database you don't change the previous scripts but add new scripts at the top of the stack.
2. There should be a single standard resources/migrations migration directory
3. If a specific platform (dev, stage, test, etc) needs additional scripts, specific directories should be created and
   in config set the appropriate migrations-dir as a vector containing the standard directory and the auxiliary
   directory.
4. The order in which scripts are executed depends only on the script id and not on the directory where the script is
   located

### Configuration

The migration process requires a config file containing:

```clojure
:xiana/postgresql {:port     5432
                   :dbname   "framework"
                   :host     "localhost"
                   :dbtype   "postgresql"
                   :user     "postgres"
                   :password "postgres"}
:xiana/migration {:store                :database
                  :migration-dir        ["resources/migrations"]
                  :init-in-transaction? false
                  :migration-table-name "migrations"}
```

The :migration-dir param is a vector of classpath relative paths containing database migrations scripts.

### Usage

The `xiana.db.migrate` implements a cli for migrations framework.

If you add to `deps.edn` in `:aliases` section:

```clojure
:migrate {:main-opts ["-m" "xiana.db.migrate"]}
```

you could access this cli from clojure command.

To see all commands and options available run:

```shell
clojure -M:migrate --help
```

Examples of commands:

```shell
# update the database to current version:
clojure -M:migrate migrate -c resources/config.edn
# rollback the last run migration script:
clojure -M:migrate rollback -c resources/config.edn
# rollback the database down until id script: 
clojure -M:migrate rollback -i 20220103163538 -c resources/config.edn
# create the migrations scripts pair: 
clojure -M:migrate create -d resources/migrations -n the-name-of-the-script
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
    (assoc state :response {:status 200
                            :body   {:view-type "login"
                                     :data      {:login   "succeed"
                                                 :user-id id}}})))
```

## Side-effects

Conventionally, side-effects interceptor is placed after [action](#action) and [database-access](#database-access),
just
right before [view](#view). At this point, we already have the result of database execution, so we are able to do some
extra refinements, like sending notifications, updating the application state, filtering or mapping the result and so
on.

This example shows you, how can you react on a login request. This stores the user data in the actual session on
successful login, or injects the `Unauthorized` response into the state.

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

### In memory session backend

Basically it's an atom backed session protocol implementation, allows you to `fetch` `add!` `delete!` `dump`
and `erase!` session data or the whole session storage. It doesn't require any additional configuration, and this is
the default set up for handling session storage. All stored session data is wiped out on system restart.

### Persistent session backend

Instead of atom, it uses a postgresql table to store session data. Has the same protocol as in-memory. Configuration is
necessary to use it.

- it's necessary to have a table in postgres:

```postgres-sql
CREATE TABLE sessions (
    session_data json not null,
    session_id uuid primary key,
    modified_at timestamp DEFAULT CURRENT_TIMESTAMP
);
```

- you need to define the session's configuration in you `config.edn` files:

```clojure
 :xiana/session-backend {:storage            :database
                         :session-table-name :sessions}
```

- in case of
    - missing `:storage` key, `in-memory` session backend will be used
    - missing `:session-table-name` key, `:sessions` table will be used

- the database connection can be configured in three ways:

    - via additional configuration

    ```clojure
     :xiana/session-backend {:storage            :database
                             :session-table-name :sessions
                             :port               5433
                             :dbname             "app-db"
                             :host               "localhost"
                             :dbtype             "postgresql"
                             :user               "db-user"
                             :password           "db-password"}
    ```

    - using the same datasource as the application use:

  Just init the backend after the database connection
    ```clojure
    (defn ->system
    [app-cfg]
    (-> (config/config app-cfg)
        routes/reset
        db-core/connect
        db-core/migrate!
        session/init-backend
        ws/start))
    ```

    - Creating new datasource

  If no datasource is provided on initialization, the `init-backend` function merges the database config with the
  session backend configuration, and creates a new datasource from the result.

### Session interceptors

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
