# acl

API implementation for ACL example

## Usage

### Prepare

start postgres with docker-compose:

    docker compose up -d

### Run application

    lein run

### Run tests

    lein test



### Try controller

#### test user-ids:

|role            |user-id                               |
|----------------|--------------------------------------|
|guest           | Optional                             |
|member          | 611d7f8a-456d-4f3c-802d-4d869dcd89bf |
|admin           | b651939c-96e6-4fbb-88fb-299e728e21c8 |
|suspended_admin | b01fae53-d742-4990-ac01-edadeb4f2e8f |
|staff           | 75c0d9b2-2c23-41a7-93a1-d1b716cdfa6c |

#### Test actions:

to select acting user, add `-H "Authorization: {{user-id}}"` to curl parameters

get all posts:

    curl http://localhost:3000/posts

get post by id:

    curl http://localhost:3000/posts?id={{post-id}}

create new post:
    
    curl -X PUT -d 'content=something to post' http://localhost:3000/posts

update existing post:

    curl -X POST -d 'content=Update post to this' http://localhost:3000/posts?id={{post-id}}

delete post:

    curl -X DELETE http://localhost:3000/posts?id={{post-id}}

