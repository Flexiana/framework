# controllers

FIXME: description

## Usage

### Run the backend

```shell
lein run
```

### Try controllers

    curl http://localhost:3000/

    Unauthorized
    
    curl http://localhost:3000/wrong

    Unauthorized

    curl http://localhost:3000/re-frame

    Unauthorized

    curl -H "Authorization: Basic YWxhZGRpbjpvcGVuc2VzYW1l" http://localhost:3000/re-frame

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset='utf-8'>
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>controllers</title>
</head>
<body>
<noscript>
    controllers is a JavaScript app. Please enable JavaScript to continue.
</noscript>
<div id="app"></div>
<script src="assets/js/compiled/app.js"></script>
</body>
</html>
```

    curl -H "Authorization: Basic YWxhZGRpbjpvcGVuc2VzYW1l" http://localhost:3000/

    Index page
    
    curl -H "Authorization: Basic YWxhZGRpbjpvcGVuc2VzYW1l" http://localhost:3000/wrong

    Not Found

    curl -H "Authorization: Basic YWxhZGRpbjpvcGVuc2VzYW1l" http://localhost:3000/api/siege-machines/1 

    {"id":1,"name":"trebuchet"}

    curl -H "Authorization: Basic YWxhZGRpbjpvcGVuc2VzYW1l" http://localhost:3000/api/siege-machines/apple 

    Request coercion failed

    curl -H "Authorization: Basic YWxhZGRpbjpvcGVuc2VzYW1l" http://localhost:3000/api/siege-machines/4

    Not found

    curl -H "Authorization: Basic YWxhZGRpbjpvcGVuc2VzYW1l" http://localhost:3000/api/siege-machines/3

    Response validation failed

### Run controllers test

Run tests with

```shell 
lein test components-test
```
