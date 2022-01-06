# Interceptors implemented in Xiana

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

## log

Prints the actual state map on both `:enter` and `:leave` phases.

## side-effect

Executes the function from state's `:side-effect` key, if there is one. It's often placed between database access and
response, to execute actions based on the database result.

## view

The view interceptor renders the response map based the given state via the provided `:view` keyword.

## params

Params for resolving the request parameters.

## db-access

Executes `:query` and `:db-queries` keys on `:leave` phase.

## message

Prints out provided message on `:leave` and on `:enter`

## muuntaja

Decodes the request and encodes the response based on request's Accept and Content-type headers

## session

On `:enter`, either injects session-data based on headers, cookies or query params' session-id, or short circuits
execution with invalid or missing session responses.

On `:leave` updates session storage with session-data from state.

## protected-session

Same behavior as [session](#session), with the addition of getting two path parameters, one to protect, and one to
exclude. For example

```clojure
(sessions/protected-session "/api" "/login")
```

## guest-session

Same as  [session](#session), except that if session is missing or not provided, creates a new session for `:guest`
user.

## rbac

On `:enter`, decides if the given user (from session-data user role) has permissions for a given action. When no
permission is found, short circuits the execution with `:status 403`.

On `:leave`, tightens the database query with given `:restriction-fn`, if any.

## coercion

On `:enter`, performs request parameter wrapping, and validates by given roles.

On `:leave`, validates the response body with the given malli schema.

## cookies

    Cookie request/response wrapper
