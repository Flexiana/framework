# Conventions

- [State](#state)
- [Monads](#monads)
- [Action](#action)
- [Handler](#handler)
- [Dependencies](#dependencies)
- [Interceptors](#interceptors)

## State

State record created for each HTTP request and represents the current state of the application. It contains:

- the application's dependencies
- request
- request-data
- response

This structure is very volatile, will be updated quite often on the application's life cycle.

The main modules that updates the state are:

- Routes:

  Add information from the match route to the state map

- Interceptors:

  Add, consumes or remove information from the state map, more details on the Interceptors section.

- Actions:

  In actions, you are able to interfere with the :leave parts of the interceptors.

As the last step of execution the handler extracts the response value from the state.

The state will be renewed for every request.

## Monads

    "Monad is a simple and powerful mechanism for function composition that helps us to solve very common
    IT problems such as input/output, exception handling, parsing, concurrency and other.
    Application becomes less error-prone. Code becomes reusable and more readable."

And we use it to do exactly that: to add Failure/Success metadata to our internal wrapped state, our data flow unity.

Think of it as a container that's composed by metadata plus its data value, and every function that returns the state
map needs to be wrapped first, providing the right binary direction on Success or Failure.

This is done by the functions: `xiana/ok` and `xiana/error` respectively defined in `xiana/core.clj`.

The container will travel through the application and dictates how it will operate based on its binary direction values
and the state map.

It's easier to get use to it, if you make an analogue for it: it looks and works like railway programming:
If the execution is flawless, the state goes through the whole link of interceptors and action, if the execution fails
at any point, the monadic system short circuits the execution, and preventing us to make any other errors.

## Action

    The action conventionally is the control point of the application flow.
    This is the place were you can define how the rest of your execution flow would behave. 
    Here you can provide the database query, restriction function, the view, and the additional side effect functions
    are you want to execute.

The actions are defined in the routing

```clojure
["/" {:get {:action #(do something)}}]
```

## Handler

    The framework provided handler is a general tool. It creates the state for every request, makes the routing with it,
    executes the interceptors, handles interceptor overrides, and not-found cases.
    It's also handles websocket requests too.

### Routing

    Routing is selecting actions to execute by the requested url, and HTTP method.

## Dependencies

    Some modules are depends on external resources, configuration, or on other modules. This dependecies are going into 
    the state, on state creation, and defined on application startup.

## Interceptors

    An interceptor is a pair of unary functions. Each function is called with a state map and must return a state map
    wrapped into a monad container. You can see it analogue to AOP's around aspect, or pair of middlewares. 
    In Xiana we're providing a handful set of interceptors for do the most common use cases.


