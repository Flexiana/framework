# cli-chat

Websockets based chat server example implementation

## Usage

### Start dockerized PostgreSQL
```shell
docker compose up -d
```
    
### Log into psql console
```shell
psql -U postgres -p 5432 -h localhost
```

### Run the backend
```shell
lein run
```

### Try cli-chat

Connect at least two times with [WebSocat](https://github.com/vi/websocat/releases) to `ws://localhost:3000/chat`
