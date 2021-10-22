# Interceptors implemented in xiana

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

## ~~acl-restrict~~

_^deprecated_

    Access control layer interceptor.
    Enter: Lambda function checks access control.    
    Leave: Lambda function place for tightening db query via provided owner-fn.

## log

    Prints the actual state map both enter and leave phase

## side-effect

    If available then executes a function from state side effect key. In default, it's placed between database access and
    response, to provide a place to put some action depending on database result.

## view

    The view interceptor renders the response map based the given state via the provided :view keyword.

## params

    Params for resolving the request parameters

## db-access

    Executes the :query key if it's provided on database on :leave state

## message

    Prints out provided message on :leave and on :enter

## session-user-id

    :enter Gets session-id from header, associates the session into state's :session-data if it's in the session storage.
    Else creates a new session
    :leave Updates session-storage from the state session-data

## session-user-role

    :enter Associates user role based on authorization header into session-data, authorization

## muuntaja

    Encoding the request, decoding response based on request's Accept and Content-type headers

## session

    :enter Inject session-data based on headers, cookies or query params session-id, or short circuits execution with
    invalid or missing session

    :leave Updates session storage with session-data from state.

## rbac

    :enter Decides if the given user (from session-data user role) has permission for a given action
    when not, short circuits the execution with status: 403
    :leave Tightens the database query with given restriction-fn if any

## coercion

    :enter Make the request parameter wrapping and validates by given roles
    :leave Validates the response body with given malli schema

## cookies

    Cookie request/response wrapper
