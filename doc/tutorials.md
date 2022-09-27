<img src="resources/images/Xiana.png" width="242">

# Tutorials

- [Application startup](#application-startup)
- [Configuration](#configuration)
- [Dependencies](#dependencies)
- [Router and controller interceptors](#router-and-controller-interceptors)
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
- [WebSockets](#websockets)
    - [WebSockets routing](#websockets-routing)
    - [Route matching](#route-matching)
- [Server-Sent Events (SSE)](#server-sent-events-sse)
- [Scheduler](#scheduler)

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

## Routes

Route definition is done via [reitit's routing](https://github.com/metosin/reitit) library. Route processing is done
with `xiana.route` namespace. At route definition you can define.

- The [action](#action) that should be executed
- [Interceptor overriding](how-to.md#interceptor-overriding)
- The required permission for [rbac](how-to.md#role-based-access-and-data-ownership-control)
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

### Configure migration

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
and support of [interceptors](conventions.md#interceptors), with [interceptor override](how-to.md#interceptor-overriding). You
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
