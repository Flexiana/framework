# migrator

FIXME: description

## Usage

### Start dockerized PostgreSQL

    ./postgres-start.sh
    
### Log into psql console

    psql -U postgres -p 5433 -h localhost

### Build frontend and run the backend

    lein release

    lein run

### Try migrator

    curl http://localhost:3000/

### Open re-frame app

    open http://localhost:3000/re-frame
    
