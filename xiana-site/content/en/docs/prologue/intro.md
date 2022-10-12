---
title: "Introduction"
description: "Xiana - like any other framework - is for helping developers, without using any magic."
lead: "Xiana - like any other framework - is for helping developers, in this case to create web applications. It
provides ready to use solutions for most common use cases. It's like getting prepared and portioned ingredients instead
of you need to peel the carrot to cook a soup. It's lightweight. No hocus-pocus is necessary to spin up the application
and maintain components life cycle, even most of the work is done by the framework. You just define the database query,
and how it should be rendered to sent via API, inject it into the app state and the work is done. You wanna follow the
user activity? Put the data you need into the app state, and it will store it for you. The goal was having an universal
way to sort out everything we faced while working on our inside projects. We're just passing through the application
state, and controlling the application behavior with the content of it."
date: 2020-10-06T08:48:57+00:00
lastmod: 2020-10-06T08:48:57+00:00
draft: false
images: []
menu:
docs:
parent: "prologue"
weight: 100
toc: true
---

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
