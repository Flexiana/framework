# Xiana framework

Xiana is a lightweight web-application framework written in clojure, for clojure. The goal is to be simple, fast, and most
important - a welcome platform for web programmers with different background who want to experience the wonders of
functional programming!

It's easy to install, fun to experiment with, and a powerful tool to produce monolithic web application.

## Installation

### From template

Xiana has its leiningen template, so you can create a skeleton project with

```shell
lein new xiana app
```

### As dependency

Add it to your project as dependency from clojars:

[![Clojars Project](https://img.shields.io/clojars/v/com.flexiana/framework.svg)](https://clojars.org/com.flexiana/framework)

## Docs

- Everyone should read about [conventions](./doc/conventions.md).
- To start working with xiana, read [tutorials](./doc/tutorials.md).
- When you interested in advised [how-to](./doc/how-to.md)s.
- Readings about provided [modules](./doc/modules.md), and [interceptors](./doc/interceptors.md).
- To contribute check [contribution](./doc/contribution.md) docs.

### Examples

Visit [examples folder](https://github.com/Flexiana/framework/tree/main/examples), and see how you can do

- [Access and data ownership control](examples/acl/README.md)
- [Request coercion and response validation](examples/controllers/README.md)
- [Session handling with varying interceptors](examples/sessions/README.md)
- [Chat platform with WebSocket]() #TODO 

## References

1. http://funcool.github.io/cats/latest/
2. https://medium.com/@yuriigorbylov/monads-and-why-do-they-matter-9a285862e8b4
3. http://pedestal.io/reference/interceptors.
