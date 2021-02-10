# frames

## Usage

### Prepare PostgreSQL
Before the first run, you must prepare the database:

    user: postgres

    password: postgres
    
    Create a database, named 'frames'

### Compile front-end and run the application

    lein release
    
    lein run

### Open the app

http://localhost:3000/

http://localhost:3000/status

should response 
{"status":"OK"}