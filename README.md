![Xiana logo](doc/resources/images/Xiana.png)

# Xiana framework

Xiana is a lightweight web-application framework written in Clojure, for Clojure. The goal is to be simple, fast, and
most importantly - a welcoming platform for web programmers with different backgrounds who want to experience the
wonders of functional programming!

It's easy to install, fun to experiment with, and a powerful tool to produce monolithic web applications.

## Installation

### From template

Xiana has its own Leiningen template, so you can create a skeleton project with

```shell
lein new xiana app
```

[getting-started](./doc/getting-started.md) explains how to use this to create a very simple app with a db, a backend
offering an API, and a frontend that displays something from the database.

### As a dependency

Add it to your project as a dependency from clojars:

[![Clojars Project](https://img.shields.io/clojars/v/com.flexiana/framework.svg)](https://clojars.org/com.flexiana/framework)

## Docs

- First check out the [conventions](./doc/conventions.md).
- To start working with xiana, read the [tutorials](./doc/tutorials.md).
- To contribute, see the [contribution](./doc/contribution.md) docs.

### Examples

Visit [examples folder](examples), to see how you can perform

- [Access and data ownership control](examples/acl/README.md)
- [Request coercion and response validation](examples/controllers/README.md)
- [Session handling with varying interceptors](examples/sessions/README.md)
- [Chat platform with WebSocket](examples/cli-chat/README.md)
- [Event based resource handling](examples/state-events/README.md)

## References

### Concept of interceptors

http://pedestal.io/reference/interceptors

https://github.com/metosin/sieppari
