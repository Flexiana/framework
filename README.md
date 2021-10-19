# Xiana framework

Xiana is a lightweight web-application framework written in Clojure LISP, its goal is to be simple, blasting fast, and
most important - a welcome platform for web programmers from different backgrounds who want to experience the wonders of
functional programming!

It's easy to install, fun to experiment and a powerful tool to produce reliable code for the world wide web.

## Contents

- [Conventions](#conventions)
  - [State](#state)
  - [Monads](#monads)
  - [Action](#action)
  - [Handler](#handler)
  - [Dependencies](#dependencies)
  - [Interceptors](#interceptors)
    - [Router and controller interceptors](#router-and-controller-interceptors)
    - [Providing default interceptors](#providing-default-interceptors)
    - [Typical use-case, and ordering](#typical-use-case-and-ordering)
    - [Interceptor overriding](#interceptor-overriding)
    - [Interceptors implemented in xiana](#interceptors-implemented-in-xiana)
      - [acl-restrict](#acl-restrict)
      - [log](#log)
      - [side-effect](#side-effect)
      - [view](#view)
      - [params](#params)
      - [db-access](#db-access)
      - [message](#message)
      - [session-user-id](#session-user-id)
      - [session-user-role](#session-user-role)
      - [muuntaja](#muuntaja)
      - [session](#session)
      - [rbac](#rbac)
      - [coercion](#coercion)
      - [cookies](#cookies)
    - [Defining new interceptors](#defining-new-interceptors)
      - [Example](#example)
- [Modules](#modules)
  - [Frontend](#frontend)
  - [Backend](#backend)
    - [Core](#core)
    - [ACL](#acl)
    - [Auth](#auth)
    - [Config](#config)
    - [Database](#database)
    - [Core](#core)
    - [Postgresql](#postgresql)
    - [SQL](#sql)
    - [Migrations](#migrations)
    - [Interceptor](#interceptor)
      - [Interceptor/core](#interceptorcore)
      - [Interceptor/muuntaja](#interceptormuuntaja)
      - [Interceptor/wrap](#interceptorwrap)
    - [Mail](#mail)
    - [RBAC](#rbac-1)
    - [Route](#route)
    - [Session](#session)
    - [State](#state-1)
    - [Webserver](#webserver)
- [Usage](#usage)
  - [From template](#from-template)
  - [As dependency](#as-dependency)
  - [Examples](#examples)
- [Contribution](#contribution)
  - [Development dependencies](#development-dependencies)
  - [Setup](#setup)
  - [Deps](#deps)
  - [Leiningen](#lein)
- [References](#references)

## Conventions

Let's talk about the conventions, naming and meanings.

### State

A simple map that is created for each HTTP request and represents the current state of the application It contains:

- the application's dependencies
- request
- request-data
- response
- session-data

This structure is very volatile, will be updated quite often on the application's life cycle.

The main modules that updates the state are:

- Routes:

  Add information from the match route to the state map

- Interceptors:

  Add, consumes or remove information from the state map, more details on the Interceptors section.

As the last step of execution the handler extracts the response value from the state.

The state will be renewed for every request.

### Monads

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

### Action

    The action conventionally is the control point of the application flow.
    This is the place were you can define how the rest of your execution flow would behave. 
    Here you can provide the database query, restriction function, the view, and the additional side effect functions
    are you want to execute.

The actions are defined in the routing

```clojure
["/" {:get {:action #(do something)}}]
```

### Handler

    The framework provided handler which is a multipurpose tool. It creates the state for every request, 
    makes the routing with it, executes the interceptors, handles interceptor overrides, and not-found cases.
    It's injects request data down execution stream.

#### Routing

    Routing is selecting actions to execute by the requested url, and HTTP method.

### Dependencies

### Interceptors

    An interceptor is a pair of unary functions. Each function is called with a state map and must return a state map
    wrapped into a monad container. You can see it analogue to AOP's around aspect, or pair of middlewares. 
    In Xiana we're providing a handful set of interceptors for do the most common use cases.

#### Router and controller interceptors

    The router and controller interceptors are executed in a same way (enter functions in order, leave functions in reversed
    order), but not in the same place of the execution flow.

The handler function executes interceptors in this order

> 1. router interceptors :enter functions in order
> 2. router interceptors :leave functions in reversed order
> 3. routing, and matching
> 4. controller interceptors :enter functions in order
> 5. action
> 6. controller interceptors :leave functions in reversed order

In router interceptors, you are able to interfere with the routing mechanism. Controller interceptors can be interfered
via route definition.

#### Providing default interceptors

The router and controller interceptors should be part of the application definition. The system's dependency map should
contain two sequence of interceptors like

```clojure
{:router-interceptors     [...]
 :controller-interceptors [...]}
```

#### Typical use-case, and ordering

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

> 1. Muuntaja do the request's encoding
> 2. parameters injected via reitit
> 3. injecting session-data into the state
> 4. view does nothing in enter
> 5. db-access does nothing on enter
> 6. RBAC tests for permissions
> 7. execution of the given action
> 8. RBAC applies data ownership function
> 9. db-access executes the given query
> 10. rendering response map
> 11. updating session storage from state/session-data
> 12. Params do nothing on leave
> 13. muuntaja converts the response body to the accepted format

#### Interceptor overriding

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

> 1. router interceptors :enters in order
>2. router interceptors :leaves in reversed order
>3. routing
>4. around interceptors :enters in order
>5. controller interceptors :enters in order
>6. inside interceptors :enters in order
>7. action
>8. inside interceptors :leaves in reversed order
>9. controller interceptors :leaves in reversed order
>10. around interceptors :leaves in reversed order

#### Interceptors implemented in xiana

The xiana framework provides you a handful set of interceptors

##### ~~acl-restrict~~

_^deprecated_

    Access control layer interceptor.
    Enter: Lambda function checks access control.    
    Leave: Lambda function place for tightening db query via provided owner-fn.

##### log

    Prints the actual state map both enter and leave phase

##### side-effect

    If available then executes a function from state side effect key. In default, it's placed between database access and
    response, to provide a place to put some action depending on database result.

##### view

    The view interceptor renders the response map based the given state via the provided :view keyword.

##### params

    Params for resolving the request parameters

##### db-access

    Executes the :query key if it's provided on database on :leave state

##### message

    Prints out provided message on :leave and on :enter

##### session-user-id

    :enter Gets session-id from header, associates the session into state's :session-data if it's in the session storage.
    Else creates a new session
    :leave Updates session-storage from the state session-data

##### session-user-role

    :enter Associates user role based on authorization header into session-data, authorization

##### muuntaja

    Encoding the request, decoding response based on request's Accept and Content-type headers

##### session

    :enter Inject session-data based on headers, cookies or query params session-id, or short circuits execution with
    invalid or missing session

    :leave Updates session storage with session-data from state.

##### rbac

    :enter Decides if the given user (from session-data user role) has permission for a given action
    when not, short circuits the execution with status: 403
    :leave Tightens the database query with given restriction-fn if any

##### coercion

    :enter Make the request parameter wrapping and validates by given roles
    :leave Validates the response body with given malli schema

##### cookies

    Cookie request/response wrapper

#### Defining new interceptors

    An interceptor is a map of three functions.
    :enter Runs while we are going down from the request to it's action, in the order of executors
    :leave Runs while we're going up from the action to the response.
    :error Executed when any error thrown while executing the two other functions

The provided function should have one parameter, the application state, and should return the state wrapped into the
xiana monad.

##### Example

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

## Modules

Xiana frameworks gives you modules to work with, let take a look for each of them

### Frontend

    Have no idea!!!

### Backend

#### Core

    Xiana.core contains the State record, the monad helpers, and flow macros to deal with the monadic structure.

#### ~~ACL~~

    ^deprecated
    use RBAC instead

#### Auth

    Auth.core is a package to deal with password encoding, and validation

#### Config

    config/core provides functions to deal with environment variables and config.edn files

#### Database

    Database handling functions

##### Core

    db.core Start function, to get the datasource based on given configuration

##### Postgresql

    Some honey-sql helpers

##### Sql

    SQL helper functions, query executor execute functions gets the datasource and query-map (honey-sql map) and executes it
    on the database

##### Migrations

    Migrations is a migration helper around migratus.

#### Interceptor

    Some provided interceptors, and other helpers

##### Interceptor/core

    Collection of interceptors, like:

    log
    side-effect
    view
    params
    db-access
    message
    session-user-id
    session-user-role
    muuntaja

##### Interceptor/muuntaja

    Default settings, and format instance of content negotiation

##### Interceptor/queue

    The queue is the interceptor executor. It contains all the functions what is necessary to the handler to deal with
    interceptors, interceptor overrides.

##### Interceptor/wrap

    With interceptor wrapper you can use any kind of interceptors and middlewares with xiana provided flow.

#### Mail

    Helps you to send an email message based on configuration

#### RBAC

    Wrapper package for tiny-RBAC lib, with initialize role-set, gathering permissions from the actual state,
    and it contains an interceptor too, to deal with permissions and restrictions.

#### Route

    Contains all functions to deal with route dependent functionality. Use reitit matcher and router. 
    Collects request-data for processing it via controller interceptors and action.

#### Session

    Contains Session protocol definition, an in-memory session backend, and the session interceptor

#### State

    In handler-fn it creates the initial state for request execution.

#### Webserver

    Starts a jetty server with the default handler, provides the dependencies to the handler function.

## Usage

### From template

Xiana has its leiningen template, so you can create a skeleton project with

```shell
lein new xiana app
```

### As dependency

Add it to your project as dependency from clojars:

[![Clojars Project](https://img.shields.io/clojars/v/com.flexiana/framework.svg)](https://clojars.org/com.flexiana/framework)

### Examples

Visit [examples folder](https://github.com/Flexiana/framework/tree/main/examples), and see how you can do

- [Access and data ownership control](examples/acl/README.md)
- [Coercion, validation](examples/controllers/README.md)
- [Session handling with varying interceptors](examples/sessions/README.md)

## Contribution

### Development dependencies

#### Mandatory

- Clojure 1.10
- Postgresql >= 11.5
- leiningen >= 2.9.0

#### Optional

- Docker >= 19.03.11
- Docker-compose >= 1.21.0

#### Libraries

##### Mandatory

| Name                            | Version | Related    |
|---------------------------------|---------|------------|
| funcool/cats                    |   2.4.1 | Monad      |
| funcool/cuerdas                 | RELEASE | Monad      |
| metosin/reitit                  |  0.5.12 | Routes     |
| potemkin/potemkin               |   0.4.5 | Helper     |
| com.draines/postal              |   2.0.4 | Email      |
| duct/server.http.jetty          |   0.2.1 | WebServer  |
| seancorfield/next.jdbc          | 1.1.613 | WebServer  |
| honeysql/honeysql               | 1.0.444 | PostGreSQL |
| nilenso/honeysql-postgres       |   0.2.6 | PostGreSQL |
| org.postgresql/postgresql       |  42.2.2 | PostGreSQL |
| crypto-password/crypto-password |   0.2.1 | Security   |

##### Optional

| Name                | Version | Provide |
|---------------------|---------|---------|
| clj-kondo/clj-kondo | RELEASE | Tests   |

### Setup

```shell
$ git clone git@github.com:Flexiana/framework.git; cd framework
$ ./script/auto.sh -y all
```

The first command will clone `Flexiana/framework` repository and jump to its directory. The second command
calls `auto.sh` script to perform the following sequence of steps:

1. Download the necessary docker images
2. Instantiate the database container
3. Import the initial SQL schema: `./docker/sql-scripts/init.sql`
4. Populate the new schema with 'fake' data from: `./docker/sql-scripts/test.sql`
5. Call `lein test` that will download the necessary *Clojure*
   dependencies and executes unitary tests.

See `./script/auto.sh help` for more advanced options.

Remember it's necessary to have `docker/docker-compose` installed in your host machine. Docker daemon should be
initialized a priori, otherwise the chain of commands fails.

It should also be noted that after the first installation everything will be cached preventing unnecessary rework, it's
possible to run only the tests, if your development environment is already up, increasing the overall productivity.

```shell
./script/auto.sh -y tests
```

### Deps

We define some aliases to make possible to use `deps.edn` directly
(recommend).

### leiningen

Using lein directly is very simple:

```shell
lein test
```

The available commands (aliases):

| Alias    | Description       |
|----------|-------------------|
| test        | Executing tests with kaocha  |
| fix-style   | fix styling with clj-style   |
| check-style | check styling with clj-style |
| pre-hook    | Executing check-style and test aliases |


## References

1. https://clojuredocs.org/clojure.edn/read
2. http://funcool.github.io/cats/latest/
3. https://medium.com/@yuriigorbylov/monads-and-why-do-they-matter-9a285862e8b4
4. http://pedestal.io/reference/interceptors.
