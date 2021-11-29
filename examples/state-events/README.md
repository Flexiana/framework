# state-events

Example application for event based resource handling. Supporting `undo`, `redo` `delete` actions.

See the tests for an example resource. try it out on frontend


## Usage

### Start dockerized PostgreSQL

    docker-compose up -d

### Log into psql console

    psql -U postgres -p 5433 -h localhost

### Build frontend and run the backend

    lein release

    lein run

### Open re-frame app

    open http://localhost:3000/re-frame