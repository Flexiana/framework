# frames

## Usage

### Start dockerized PostgreSQL

    docker compose up -d


### Compile front-end and run the application

    lein release
    
    lein run

### Open the app

http://localhost:3000/

http://localhost:3000/status

should response
{"status":"OK"}
