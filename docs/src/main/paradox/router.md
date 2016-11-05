# Router
Akka Wamp provides you with a basic Router that can be either launched as standalone server process or embedded into your application. It implements:

* WAMP Basic Profile,
* __Broker__ and __Dealer__ roles,
* JSON serialization,
* WebSocket transport

@@@warning
Though perfectly functional, Akka Wamp Router is intended for development purposes only. You're advised to adopt production ready WAMP routers such as [Crossbar.IO](http://crossbar.io/)
@@@

## Standalone
[![Download][download-image]][download-url]

Download the latest version, extract, configure and run it as standalone application:

tgz
:   @@snip [install.sh](./tgz.sh)



## Embedded
Create and bind an embedded router passing an actor reference factory (such as a brand new actor system or any of your actor context)

scala
:    @@snip [EmbeddedRouter.scala](../../../../examples/router/examples/ScalaRouterApp.scala)

java
:    @@snip [EmbeddedRouter.java](../../../../examples/router/examples/JavaRouterApp.java)

### Internals
A Binder actor spawns a Router actor to be bound to the Akka IO Wamp Manager. The manager spawns one ConnectionListener actor which listens for incoming connection requests. The listener spawns a new ConnectionHandler actor upon each connection establishment to serve a specific client. 

![router](router.png)

## Configuration

@@snip[application.conf](../../../../core/src/main/resources/reference.conf){ #router }

[download-image]: https://api.bintray.com/packages/angiolep/universal/akka-wamp/images/download.svg
[download-url]: https://bintray.com/angiolep/universal/akka-wamp/_latestVersion

