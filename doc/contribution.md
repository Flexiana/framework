# Contribution

- [Development dependencies](#development-dependencies)
- [Setup](#setup)
- [Deps](#deps)
- [CLJ-tools](#clj-tools)

## Development dependencies

### Mandatory

- Clojure 1.10
- Postgresql >= 11.5 
- Clojure cli >= 1.11.1.1155
- Docker >= 19.03.11
- Docker-compose >= 1.21.0

### Libraries

#### Mandatory

| Name                            | Version | Related             |
|---------------------------------|---------|---------------------|
| funcool/cuerdas                 | RELEASE | String manipulation |
| metosin/reitit                  | 0.5.12  | Routes              |
| potemkin/potemkin               | 0.4.5   | Helper              |
| com.draines/postal              | 2.0.4   | Email               |
| duct/server.http.jetty          | 0.2.1   | WebServer           |
| seancorfield/next.jdbc          | 1.1.613 | WebServer           |
| honeysql/honeysql               | 1.0.444 | PostGreSQL          |
| nilenso/honeysql-postgres       | 0.2.6   | PostGreSQL          |
| org.postgresql/postgresql       | 42.2.2  | PostGreSQL          |
| crypto-password/crypto-password | 0.2.1   | Security            |

#### Optional

| Name                | Version | Provide |
|---------------------|---------|---------|
| clj-kondo/clj-kondo | RELEASE | Tests   |

## Setup

```shell
$ git clone git@github.com:Flexiana/framework.git; cd framework
$ ./script/auto.sh -y all
```

The first command will clone `Flexiana/framework` repository and jump to its directory. The second command
calls `auto.sh` script to perform the following sequence of steps:

1. Download the necessary docker images
2. Instantiate the database container
3. Import the initial SQL schema: `./docker/sql-scripts/init.sql`
4. Populate the new schema with 'fake' data from: `./docker/sql-scripts/test.sql`
5. Call `clj -X:test` that will download the necessary *Clojure*
   dependencies and executes unitary tests.

See `./script/auto.sh help` for more advanced options.

Remember it's necessary to have `docker` & `docker-compose` installed in your host machine. Docker daemon should be
running. The chain of commands fails otherwise. It should also be noted that after the first installation everything
will be cached preventing unnecessary rework, it's possible to run only the tests, if your development environment is
already up, increasing the overall productivity.

```shell
./script/auto.sh -y tests
```

## CLJ-tools

Check `deps.edn` for aliases 
