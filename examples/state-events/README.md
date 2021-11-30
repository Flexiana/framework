# state-events

Example for event resourcing, and server sent event based frontend state management.

## Description

The actual resource is the aggregate of the events. You can create a resource with `PUT` modify it with `POST` and mark
as deleted with `DELETE` methods. Every events can be undone except creation, or the last change was from another
session. Undo events can redoed in (with) the same session(-id). It possible to remove fields on by one (except resource
id, and resource type), to clean up a resource.

## Start dockerized PostgreSQL

    docker-compose up -d

## Log into psql console

    psql -U postgres -p 5433 -h localhost

## Build frontend and run the backend

    lein release

    lein run

## Open re-frame app

- open [the app](http://localhost:3000/re-frame)
- Add new person, select it from the list above, try to apply different changes on it
- Open another session in private browser, or another browser, to see if SSE works
- check for cookies in the developer tools, it shoud have a session-id key with UUID value