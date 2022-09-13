<img src="resources/images/Xiana.png" width="242">

# Contribution

- [Ticket system](#ticket-system)
- [Coding standards](#coding-standards)
- [Submitting a PR](#submitting-a-pr)
- [Development dependencies](#development-dependencies)
- [Setup](#setup)
- [Deps](#deps)
- [Releasing](#releasing)
- [Generating API docs](#generating-api-docs)

## Ticket system

We're using GitHub [issues](https://github.com/Flexiana/framework/issues) for tracking and discussing ideas and
requests.

## Coding standards

Please follow `clj-style` and `kondo` instructions. `Kibit` isn't a showstopper, but PRs are more welcomed if not
breaking `kibit`.

## Submitting a PR

Before you are submitting a PR be sure:

- You've updated the documentation and the [CHANGELOG](../CHANGELOG.md)
- the PR has an issue in GitHub, with a good description
- you have added tests
- you provided an example project for a new feature
- All PRs need at least two approvals
- Follow [Semantic Versioning 2.0.0](https://semver.org/#semantic-versioning-200)

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
| clj-kondo/clj-kondo             | RELEASE | Tests               |
| npx                             | RELEASE | Documentation       |

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

## Releasing

### Install locally

```shell
clj -M:install
```

### Deploying a release

- Set up a new version number in `release.edn` eg: `"0.5.0-rc2"`
- Make a git TAG with `v` prefix, like `v0.5.0-rc2`
- Push it and wait for deployment to clojars

## Executing example's tests

- Be sure all examples has the same framework version as it is in `release.edn` as dependency
- Execute `./example-tests.sh` script. It will install the actual version of xiana, and go through the examples folder
  for `check-style` and `lein test`.

## Generating API Docs

This is done with using [mermaid-cli](https://github.com/mermaid-js/mermaid-cli) and a forked version
of [Codox](https://github.com/Flexiana/codox).

We're using mermaid-cli for render UML-diagrams in markdown files, see the `doc/conventions_template.md` for example.
These files need to be added to the `/script/build-docs.sh` . For using it you need to have `npx`.

Codox is forked because markdown anchors aren't converted to HTML anchors in the official release. For use, you need to

```shell
git clone git@github.com:Flexiana/codox.git
cd codox/codox
lein install
```

it before generating the documentation.

To generate or update the current version run the script:

```shell
./script/build-docs.sh
```

This runs the following:

```shell
npx -py @mermaid-js/mermaid-cli mmdc -i doc/conventions_template.md -o doc/conventions.md
clj -X:codox
mv docs/new docs/{{version-number}}
```

It also updates the index.html file to point to the new version.