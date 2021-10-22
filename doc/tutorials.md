# Tutorials

- Dependencies
- Configuration
- [Interceptors typical use-case, and ordering](#interceptors-typical-use-case-and-ordering)
- [Defining new interceptors](#defining-new-interceptors)
    - [Interceptor example](#interceptor-example)
- [Router and controller interceptors](#router-and-controller-interceptors)
- [Providing default interceptors](#providing-default-interceptors)
- [Interceptor overriding](#interceptor-overriding)
- Routes
- Action
- Database-access
- View
- Side-effects
- User management
- Login/Logout
- Session management
- Role based access and data ownership control
- WebSocket

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

