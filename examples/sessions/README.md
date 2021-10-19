# sessions

Example that showcase the use of session interceptors using Xiana framework.

## Usage

`lein install` in the framework's root directory

### Run the backend

```bash 
lein run
```    

### Run manual tests against the application

There are 4 endpoints provided:

- localhost:3000/
>if you don't have valid session it returns
>
> ```clojure
> {:status 200, :body "Index page"}
> ```
>
>with valid session it returns
>
> ```clojure
> {:status 200, :body "Index page, for Piotr"}
> ```

- localhost:3000/secret

>if you don't have valid session it returns
>
> ```clojure
> {:status 401, :body "Invalid or missing session"}
> ```
>
>with valid session it returns
>
> ```clojure
> {:status 200, :body "Hello Piotr"}
> ```

- localhost:3000/login 
>request should look like:
>```clojure
>  {:method :post
>   :body {:email "piotr@example.com"
>          :password "topsecret"}}
>```
>returns:
>```clojure
>{:status 200
> :body {:session-id {{session-id}}
>        :user {"first-name" "Piotr"
>               "id" 1 
>               "email" "piotr@example.com"
>               "last-name" "Developer"}}}
>```
>
>Without the request body, or with wrong HTTP method it returns:
> ```clojure
> {:status 401
>  :body "Missing credentials"}
>```

- localhost:3000/logout

>if you have valid session it returns
>
> ```clojure
> {:status 200 :body "Piotr logged out"}
> ```
> 
> and it clears the session you had.

You can provide the session-id from login response in the request's headers
>```clojure
> {:headers {:session-id {{session-id}}}}
>```

### Run integration tests

```bash 
lein test
```
