# controllers

FIXME: description

## Usage

### Start dockerized PostgreSQL

    postgres-start.sh

### Build frontend and run the backend

    lein release

    lein run

### Open re-frame app

    open http://localhost:3000/re-frame

### Try controllers

    curl http://localhost:3000/
    Unauthorized
    
    curl http://localhost:3000/wrong
    Not Found
    
    curl -H "Authorization: Basic YWxhZGRpbjpvcGVuc2VzYW1l" http://localhost:3000/
    Index page
    
    curl -H "Authorization: Basic YWxhZGRpbjpvcGVuc2VzYW1l" http://localhost:3000/wrong
    Not Found
    
### Run controllers test

    Start the db with ./postgres-start.sh and run `lein test components.main-test`
