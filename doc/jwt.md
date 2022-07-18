# JWT Implementation in Xiana

- [Overview](#Overview)
- [Config](#Config)
- [jwt methods](#jwt methods)
- [interceptors](#interceptors)


## Overview

The JWT module in Xiana allows for signing and verification of JW Tokens by using the [buddy sign](https://github.com/funcool/buddy-sign) library. 

It implements two methods of both features, explained later on. There are also default interceptors for both methods, which can be found in `src/xiana/interceptor.clj` and their documentation in the [interceptors documentation](./doc/interceptors.md).

There is an example project on how to use the xiana JWT interceptors in `examples/jwt`.

[More on how JWT works](https://jwt.io/introduction).

## Config

In order to use the interceptors implemented in xiana, the config map should look something like the following:
```clojure
  {:xiana/jwt
   {:auth
    {:public-key <PUBLIC_KEY_1>
     :private-key <PRIVATE_KEY_1>
     :alg :rs256
     :in-claims {:iss "xiana-api"
                 :aud "api-consumer"
				 :sub "example-subject"
                 :leeway 0
                 :max-age 40}
     :out-claims {:exp 1000
                  :iss "xiana-api"
				  :sub "example-subject"
                  :aud "api-consumer"
                  :nbf 0}}
    :content
    {:private-key <PRIVATE_KEY_2>
	 :public-key <PUBLIC_KEY_3>
     :alg :rs256}}}
```

The top level keys `:auth` and `:content` are used to specify the configuration for `jwt-auth` and `jwt-content` interceptors respectively.

This config is better passed as an env variable, so you won't need to version control the private/public keys and instead can rely on a secret vault to set the env variable correctly [acording to the config lib](https://github.com/yogthos/config).

### Config keys

- `:alg` key is used to inform which algorithm the JWT is/will be signed [More on accepted algorithms](https://funcool.github.io/buddy-sign/latest/01-jwt.html).

- `:private-key` informs the private-key in which the JWT will be signed.

- `:public-key` informs the public-key needed to verify the JWT.

Example to generate priv/public keys (rsa256):

Do not add a passphrase for the keys.
```sh
ssh-keygen -t rsa -b 4096 -m PEM -f rs256.key
openssl rsa -in rs256.key -pubout -outform PEM -out rs256.key.pub
```


- `:out-claims` key contains keys to inform claims when signing the JWT.
  * `:exp` and `:nbf` (expiration and not before, respectively) should be a value in seconds and when the JWT is beign signed will be added to the current time. `:exp` sets the token's expiration, while `:nbf` tells the validator to not accept the token before the time provided by it.

- `:in-claims` key has the keys to validate the JWT claims.
  * `:leeway` and `:max-age`  should be a value in seconds. The `:leeway` key is used to give a "margin of error" in not-before and expiration claims validation. `:max-age` is another way to set an expiration on the JWT, it will compare the `:iat` (issued at time) claim with the current time, and if it has extrapolated the seconds set in `max-age`, the validation will fail.

Keep in mind that all claim keys are optional, but if they are present in the signed JWT, the methods will try to validate them.


## jwt methods

The `verify-jwt` and `sign` methods have two implementations: `:claims` and `:no-claims`.

The `:claims` one will use the `:out-claims` config to sign the JWT and `:in-claims` config to validate the JWT, while the `:no-claims` method will not need those configurations to be there. If using the `:no-claims` method of `verify-jwt`, the keys must not be present in the JWT, otherwise the validation will fail (with the exception of `:exp` and `:nbf`).


## interceptors

Please refer to the [interceptors]("./doc/interceptors.md") documentation.
