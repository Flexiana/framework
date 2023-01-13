# JWT Authentication

Example that showcase the use of jwt interceptors using Xiana.


## Run the backend

```bash 
clj -M:run
```

## Run manual tests against the application

In this example, 3 endpoints are provided:

#### localhost:3000/login

This is the only unprotected endpoint, meaning this endpoint will require no authorization header with a jwt token. This endpoint is the one that will provide you with the token to be put in the header.

Make a request to this endpoint by running the following:
```bash
curl -d '{"password":"topsecret", "email":"xiana@test.com"}' -H "Content-Type: application/json" -X POST http://localhost:3000/login
```

Its response will be a JSON containing `auth-token` with the JWT-token as follows:
```json
{"auth-token": "eyJhbGciOiJSUzI1NiJ9.eyJlbWFpbCI6InhpYW5hQHRlc3QuY29tIiwiYXVkIjoiYXBpLWNvbnN1bWVyIiwibGFzdC1uYW1lIjoiRGV2ZWxvcGVyIiwiaXNzIjoieGlhbmEtYXBpIiwiZXhwIjoxNjU3MDE0MDU2LCJmaXJzdC1uYW1lIjoiWGlhbmEiLCJuYmYiOjE2NTcwMTMwNTYsImlkIjoxLCJpYXQiOjE2NTcwMTMwNTZ9.Ek1UQLbhhi0cRYGLJW1y8ZnlUfcI4Kkj_6-GFNzpdbc8Ne4ovl49n6-y99mgR2cvsgI4eG9OJ6MSLUedMvyEkY7ekSFIHzdz0UGUN6k8fVPJ9wUjyvnRiQAuZoC5GwgbLAUPMedQO8oMleEHKZAABtnS4mPI-2i9tDWrG21TgFafHrL1v7GSqoQTgDz85RfMP0um53brE6FIDvSMezPewJRLtiWp4Oh1eioa-nJ0UmilMc7FMNI0BZ3PIYt2BETlDvK2Bb-ofgMXHD7C6Dx7zrHavt4bzuYAQgDzhVEIUgzc58VVdSeEgXifeG8zVtJFuOQd7kBWEkbi26l1LAdUR8IXijdIIuBffnqYnqjAIt3c5LRCxl14Ab4GXripWNSMFe8nDbw2t2ZLoXvz_P1ZnnyTHR2qotqyHP3QlxAJYLl0hMerBr5tN5nbPiL-3CBfSFMWoDyxmqHl29ewhTNoxgiy9FuVUqYzS6uE8UZ6Qd348YbvDAgoUGt9FAzqlqfWbKdWIu36W2JuWYEDHksCBZl-mBu0IDBDeVbqgwbs2o4q0Hw5BbtxiG_68KkPDh-SFgLOlp_kBWmYaHQOi6iHYLuuamCo8mjGEgon6EqaquJqlYkNbMoPXMlc1iI3WySisMdESTD-WkV2wL2u20x4zmxXGsFUy0Vpj8xZKsFJ0A0"}
```


#### localhost:3000/secret
After receiving the response from the `/login` endpoint, you should grab the content of the `auth-token` and put it in the authorization header to make requests to this endpoint, as shown below:

```bash
curl -d '{"example":"payload"}' -H "Content-Type: application/json" -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9.eyJlbWFpbCI6InhpYW5hQHRlc3QuY29tIiwiYXVkIjoiYXBpLWNvbnN1bWVyIiwibGFzdC1uYW1lIjoiRGV2ZWxvcGVyIiwiaXNzIjoieGlhbmEtYXBpIiwiZXhwIjoxNjY0MjkxOTk0LCJmaXJzdC1uYW1lIjoiWGlhbmEiLCJuYmYiOjE2NjQyOTA5OTQsImlkIjoxLCJpYXQiOjE2NjQyOTA5OTR9.ndRCEjGhZ13LDu4hkrCDuBS4ZrPOyCLjGIJY5-T-cUiZ2X1c2w7nQZc9LnTDce1hxwgD9I3lueKmmZthUazO6qWlulBVOfgw1dMrA9fHA4vXJgCXMAFAPca5nyv2QNMazQpwqZeCRCMvJSzgGLE_qx7Z1LawLFQ_Bc8wjjbG-IVY-0VdiyiePTRCVw-d3i-84_IwG5VO8mh5xcTYkRb1miO2HDaOX0gZPZex2FpicUTJh3xQXEtIsBICgfBz3O8R3IENI6Ham2ZP9OiSqAbeo0qcLKQsUmASma_aOinZdV3NPvvz1v4uuE71VKL7dx96gb0x39WyKsM0EGhTOPPICE6RR-h8BB9r0i9n2elKGRGsziIpKtF83E3SZRVzEDFA77ha4zlziRCqi9ip0yadAncRc-zjuYhUpvmWb62HwOgnOjZHjePAhUiAHaS9hNFTkiiEvMeX9bN6tLN-C9DtphLcKKqsNKRkJVKlyYgxSqnJhWAe2oxlWGlm04YjjOQuMNwMm_gCEZpgIl58JXwCaQNS7WOgHHMID3Ah0Fb_I8THwK68TOwn-yJHMIlO_854CsPNXdaj4JMNfvulaR9z49GhImHEkKVXvImqMFFDjDOaRGW25-ql8V2VS1BdsRYA2mqSFSkU7TJyrBCYofTt1lTRRMXbKZymG42gfc1ch5E" -X POST http://localhost:3000/secret
```

This endpoint, if the request is successful, is supposed to have the following response body:

```
Hello Xiana. request content: {:example "payload"}
```

