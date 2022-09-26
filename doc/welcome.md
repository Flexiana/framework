<img src="resources/images/Xiana.png" width="242">

# Xiana framework

Xiana is a lightweight web-application framework written in Clojure, for Clojure. The goal is to be simple, fast, and
most importantly - a welcoming platform for web programmers with different backgrounds who want to experience the
wonders
of functional programming!

It's easy to install, fun to experiment with, and a powerful tool to produce monolithic web applications.

## Installation

### From template

Xiana has its own Leiningen template, so you can create a skeleton project with

```shell
lein new xiana app
```

### As a dependency

Add it to your project as a dependency from clojars:

[![Clojars Project](https://img.shields.io/clojars/v/com.flexiana/framework.svg)](https://clojars.org/com.flexiana/framework)

## Getting started

This [document](./getting-started.md) explains how to use Xiana to create a very simple app with a db, a backend
offering an API, and a frontend that displays something from the database.

## Docs

- First check out the [conventions](./conventions.md).
- To start working with xiana, read the [tutorials](./tutorials.md).
- To contribute, see the [contribution](./contribution.md) docs.

### Examples

Visit [examples folder](https://github.com/Flexiana/framework/tree/main/examples), to see how you can perform

- Access and data ownership control
- Request coercion and response validation
- Session handling with varying interceptors
- Chat platform with WebSockets
- Event based resource handling

## References

### Concept of interceptors

[Pedestal](http://pedestal.io/reference/interceptors)

[Sieppari](https://github.com/metosin/sieppari)
