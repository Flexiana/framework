# Interceptors implemented in Xiana

- [~~acl-restrict~~](#acl-restrict)
- [log](#log)
- [side-effect](#side-effect)
- [view](#view)
- [params](#params)
- [db-access](#db-access)
- [message](#message)
- [muuntaja](#muuntaja)
- [session](#session)
- [protected-session](#protected-session)
- [guest-session](#guest-session)
- [rbac](#rbac)
- [coercion](#coercion)
- [cookies](#cookies)

## ~~acl-restrict~~

_^deprecated_

Access control layer interceptor.

`Enter:` Lambda function checks access control.    
`Leave:` Lambda function place for tightening db query via provided owner-fn.

## log

Prints the actual state map both `:enter` and `:leave` phase.

## side-effect

If available then executes a function from states `:side-effect` key. In default, it's placed between database access
and response, to provide a place where to put some action depending on database result.

## view

The view interceptor renders the response map based the given state via the provided `:view` keyword.

## params

Params for resolving the request parameters.

## db-access

Executes the `:query` key if it's provided on database on `:leave` state

## message

Prints out provided message on `:leave` and on `:enter`

## muuntaja

Encoding the request, decoding response based on request's Accept and Content-type headers

## session

On `:enter` Inject session-data based on headers, cookies or query params session-id, or short circuits execution with
invalid or missing session

On `:leave` updates session storage with session-data from state.

## protected-session

Same behavior like [session](#session), but gets two path parameters, one to protect, and one to exclude. For example
for protecting `/api/*` except `/api/login`.

```clojure
(sessions/protected-session "/api" "/login")
```

## guest-session

Same as  [session](#session), except if the session is missing, or not provided creates a new session for `:guest` user.

## rbac

On `:enter` decides if the given user (from session-data user role) has permission for a given action
when not, short circuits the execution with `:status 403`

On `:leave` tightens the database query with given `:restriction-fn` if any

## coercion

On `:enter` make the request parameter wrapping and validates by given roles

On `:leave` validates the response body with given malli schema

## cookies

    Cookie request/response wrapper
