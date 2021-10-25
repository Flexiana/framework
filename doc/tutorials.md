# Tutorials

- [Dependencies and configuration](#dependencies-and-configuration)
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
- [WebSocket](#websocket)

## Dependencies and configuration

Almost all components what you need on runtime should be reachable via the passed around state. To achieve this it
should be part of the :deps map in the state. Any other configuration what you need in runtime should be part of this
map too.

The system configuration and start-up:

```clojure
(defn system
  [config]
  (let [deps {:webserver               (:framework.app/web-server config)
              :routes                  (routes/reset (:routes config))
              :role-set                (:framework.app/role-set config)
              :auth                    (:framework.app/auth config)
              :session-backend         (session-backend/init-in-memory)
              :router-interceptors     []
              :web-socket-interceptors []
              :controller-interceptors []
              :db                      (db-core/start
                                         (:framework.app/postgresql config))}]
    (update deps :webserver (ws/start deps))))
```

## Interceptors typical use-case, and ordering

Typical use-case, and ordering looks like this:

```clojure
{:router-interceptors     [app/route-override?]
 :controller-interceptors [(interceptors/muuntaja)
                           interceptors/params
                           session/interceptor
                           interceptors/view
                           interceptors/db-access
                           rbac/interceptor]}
```

Which means:

1. executes app/route-override :enter function
2. executes app/route-override :leave function
3. The router injects :request-data, and decides what action should be executed
4. Muuntaja do the request's encoding
5. parameters injected via reitit
6. injecting session-data into the state
7. view does nothing on :enter
8. db-access does nothing on :enter
9. RBAC tests for permissions
10. execution of the given action
11. RBAC applies data ownership function
12. db-access executes the given query
13. rendering response map
14. updating session storage from state/session-data
15. Params do nothing on :leave
16. muuntaja converts the response body to the accepted format

## Defining new interceptors

    An interceptor is a map of three functions.
    :enter Runs while we are going down from the request to it's action, in the order of executors
    :leave Runs while we're going up from the action to the response.
    :error Executed when any error thrown while executing the two other functions

The provided function should have one parameter, the application state, and should return the state wrapped into the
xiana monad.

### Interceptor example

```clojure

{:enter (fn [state]
          (println "Enter: " state)
          (xiana/ok state))
 :leave (fn [state]
          (println "Leave: " state)
          (xiana/ok state))
 :error (fn [state]
          (println "Error: " state)
          (xiana/error (assoc state :response {:status 500 :body "Error occurred while printing out state"})))}
```

#### Router and controller interceptors

    The router and controller interceptors are executed in a same way (enter functions in order, leave functions in reversed
    order), but not in the same place of the execution flow.

The handler function executes interceptors in this order

1. router interceptors :enter functions in order
2. router interceptors :leave functions in reversed order
3. routing, and matching
4. controller interceptors :enter functions in order
5. action
6. controller interceptors :leave functions in reversed order

In router interceptors, you are able to interfere with the routing mechanism. Controller interceptors can be interfered
via route definition.

## Providing default interceptors

The router and controller interceptors definition is part of the application startup. The system's dependency map should
contain two sequence of interceptors like

```clojure
{:router-interceptors     [...]
 :controller-interceptors [...]}
```

## Interceptor overriding

On route definition you can interfere with the default controller interceptors. With the route definition you are able
to set up a different controller interceptors what is already defined with the app. There are three ways to do it:

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

will skipp the excepted interceptors from defaults

The execution flow will look like this

1. router interceptors :enters in order
2. router interceptors :leaves in reversed order
3. routing
4. around interceptors :enters in order
5. controller interceptors :enters in order
6. inside interceptors :enters in order
7. action
8. inside interceptors :leaves in reversed order
9. controller interceptors :leaves in reversed order
10. around interceptors :leaves in reversed order

If any of interceptors are in :except will be skipped.

## Routes

Route definition is done via [reitit's routing](https://github.com/metosin/reitit) library. Route processing is done
with `framework.route.core` namespace. At route definition you can define.

- The [action](#action) what should be executed
- [Interceptor overriding](#interceptor-overriding)
- The required permission for [rbac](#role-based-access-and-data-ownership-control)
- [WebSocket](#websocket) action definition

If any extra is provided here, it goes to

```clojure
(-> state :request-data :match)
```

in routing step.

## Action

The action function in a single
[CRUD application](https://en.wikipedia.org/wiki/Create,_read,_update_and_delete#RESTful_APIs) is for define a
[view](#view), a [database-query](#database-access) and optionally a [side-effect](#side-effects) function which will be
executed in the following interceptor steps.

```clojure
(defn action
  [state]
  (xiana/ok
    (assoc state :view view/success
                 :side-effect behaviour/update-sessions-and-db!
                 :query model/fetch-query)))
```

## Database-access

The `database.core`'s interceptor extracts the datasource from the provided state parameter, and the :query.

The query should be in [honey SQL](https://github.com/nilenso/honeysql-postgres) format, it will be sql-formatted on
execution:

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

The execution always has `{:return-keys true}` parameter and the result goes into

```clojure
(-> state :response-data :db-data)
```

without any transformation.

## View

A view is a function to prepare the response final response into the state based on whatever happened before.

```clojure
(defn success
  [state]
  (let [{:users/keys [id]} (-> state :response-data :db-data first)]
    (xiana/ok
      (assoc state :response {:status  200
                              :headers {"Content-type" "Application/json"}
                              :body    {:view-type "login"
                                        :data      {:login   "succeed"
                                                    :user-id id}}}))))
```

## Side-effects

Conventionally side-effects interceptor placed after the [action](#action) and [database-access](#database-access), just
right before [view](#view). Here you already have the result of database execution, so you are able to do some extra
refinements, like sending notifications, updating the application state, filtering or mapping the result and so on.

To keep continue with the previous examples:

```clojure
(defn update-sessions-and-db!
  "Creates and adds a new session to the server's store for the user that wants to sign-in.
   Avoids duplication by firstly removing the session that is related to this user (if it exists).
   After the session addition, it updates the user's last-login value in the database."
  [state]
  (if (valid-credentials? state)
    (let [new-session-id (str (UUID/randomUUID))
          session-backend (-> state :deps :session-backend)
          {:users/keys [id] :as user} (-> state :response-data :db-data first)]
      (remove-from-session-store! session-backend id)
      (xiana-sessions/add! session-backend new-session-id user)
      (update-user-last-login! state id)
      (xiana/ok
        (assoc-in state [:response :headers "Session-id"] new-session-id)))
    (xiana/error (c/not-allowed state))))
```

## Session management

The actual session interceptor loads and saves the actual session data from its backend to the app state.

On `:enter` it loads the session by its session-id, into `(-> state :session-data)`

The session-id can be provided in headers, cookies, or as query-param. When the session-id is not provided or invalid
UUID, or the session is not stored in the storage, then the response will be:

```clojure
{:status 401
 :body   "Invalid or missing session"}
```

On the `:leave` branch, updates session storage with the data from `(-> state :session-data)`

## Role based access and data ownership control

## WebSocket