
# Modules

- Frontend
- [Backend](#backend)
    - [Core](#core)
    - [ACL](#acl)
    - [Auth](#auth)
    - [Config](#config)
    - [Database](#database)
      - [Database/core](#databasecore)
      - [Database/postgresql](#databasepostgresql)
      - [Database/sql](#databasesql)
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
    
## Backend

### Core

    Xiana.core contains the State record, the monad helpers, and flow macros to deal with the monadic structure.

### ~~ACL~~

    ^deprecated
    use RBAC instead

### Auth

    Auth.core is a package to deal with password encoding, and validation

### Config

    config/core provides functions to deal with environment variables and config.edn files

### Database

    Database handling functions

#### Database/core

    db.core Start function, to get the datasource based on given configuration

#### Database/postgresql

    Some honey-sql helpers

#### Database/sql

    SQL helper functions, query executor execute functions gets the datasource and query-map (honey-sql map) and executes it
    on the database

#### Migrations

    Migrations is a migration helper around migratus.

### Interceptor

    Some provided interceptors, and other helpers

#### Interceptor/core

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

#### Interceptor/muuntaja

    Default settings, and format instance of content negotiation

#### Interceptor/queue

    The queue is the interceptor executor. It contains all the functions what is necessary to the handler to deal with
    interceptors, interceptor overrides.

#### Interceptor/wrap

    With interceptor wrapper you can use any kind of interceptors and middlewares with xiana provided flow.

### Mail

    Helps you to send an email message based on configuration

### RBAC

    Wrapper package for tiny-RBAC lib, with initialize role-set, gathering permissions from the actual state,
    and it contains an interceptor too, to deal with permissions and restrictions.

### Route

    Contains all functions to deal with route dependent functionality. Use reitit matcher and router. 
    Collects request-data for processing it via controller interceptors and action.

### Session

    Contains Session protocol definition, an in-memory session backend, and the session interceptor

### State

    In handler-fn it creates the initial state for request execution.

### Webserver

    Starts a jetty server with the default handler, provides the dependencies to the handler function.

