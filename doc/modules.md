<img src="resources/images/Xiana.png" width="242">

# Modules

- [Auth](#auth)
- [Config](#config)
- [Database](#database)
    - [Database/main](#databasemain)
    - [Database/core](#databasecore)
- [Migrations](#migrations)
- [Interceptor](#interceptor)
    - [Interceptor/core](#interceptorcore)
    - [Interceptor/muuntaja](#interceptormuuntaja)
    - [Interceptor/wrap](#interceptorwrap)
- [Mail](#mail)
- [RBAC](#rbac-1)
- [Route](#route)
- [Scheduler](#scheduler)
- [SSE](#sse)
- [Session](#session)
- [State](#state)
- [Webserver](#webserver)



## Auth

Auth.core is a package to deal with password encoding, and validation.

## Config

config/core provides functions to deal with environment variables and config.edn files.

## Database

Database handling functions.

## Database/main

Migratus wrapper to get rid of lein migratus plugin. As well as support profile dependent configuration of migratus.

## Database/core

Start function, which gets the data source based on given configuration. Query executor functions and the db-access
interceptor.

## Interceptor

Some default interceptors and helpers.

## Interceptor/core

Collection of interceptors, like:

- log
- side-effect
- view
- params
- message
- session-user-id
- session-user-role
- muuntaja

## Interceptor/muuntaja

Default settings, and format instance of content negotiation.

## Interceptor/queue

The queue is the interceptor executor. It contains all functions necessary to the handler to deal with interceptors and
interceptor overrides.

## Interceptor/wrap

With interceptor wrapper you can use any kind of interceptors and middlewares with xiana provided flow.

## Mail

Helps you to send an email message based on configuration.

## RBAC

Wrapper package for tiny-RBAC lib. It initializes role-set and gathers permissions from the actual state. It contains an
interceptor too, to deal with permissions and restrictions.

## Route

Contains all functions to deal with route dependent functionality. Uses reitit matcher and router. Collects request-data
for processing it via controller interceptors and action.

## Scheduler

Repeated function execution. The function gets `:deps` as parameter.

## SSE

Server-sent events implementation based on HTTP-kit's Channel protocol.

## Session

Contains Session protocol definition, an in-memory, and persistent (postgres) session backend, and the session
interceptor.

## State

In handler-fn it creates the initial state for request execution.

## Webserver

Starts a jetty server with the default handler, provides the dependencies to the handler function.
