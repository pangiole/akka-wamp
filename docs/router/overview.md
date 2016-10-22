# Router
Akka Wamp provides you with a router that can be either launched as standalone server process or embedded into your application.

It provides:

* Both __broker__ (for publish/subscribe) and __dealer__ (for routed remote procedure calls) roles.
* JSON serialization



## Standalone router
[![Download][download-image]][download-url]

Download the latest router version, extract, configure and run it as standalone application:

```bash
curl https://dl.bintray.com/angiolep/universal/akka-wamp-0.11.0.tgz
tar xvfz akka-wamp-0.11.0.tar.gz
cd akka-wamp-0.11.0
vim ./conf/application.conf
./bin/akka-wamp -Dakka.loglevel=DEBUG
```


## Embedded router
Make your build depend on the latest version of akka-wamp: 

```scala
scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.github.angiolep" %% "akka-wamp" % "0.11.0"
  // ...
)
```


Create the Akka ``ActorSystem`` and the Akka Wamp ``Router`` actor as follows:

```scala
import akka.actor._
import akka.wamp.router._

implicit val system = ActorSystem("myapp")
val router = system.actorOf(Router.props(), "router")
```

![router](router.png)

### Bind/Unbind
To bind a router a router to a configured transport, just send a ``Bind`` command to the ``IO(Wamp)`` extension manager:

```scala
import akka.wamp._
import akka.wamp.messages._

val manager = IO(Wamp)
manager ! Bind(router, transport = "default")
```

The manager will spawn a new transport listener for the given transport name as per above configuration. Your application actor, the binder, will be notified by the manager about the outcome of the command:

```scala
override def receive: Receive = {
  case signal @ CommandFailed(cmd, ex) =>
    log.warning(s"$cmd failed because of $ex")

  case signal @ Bound(listener, url) =>
    log.debug(s"$listener bound to $url")
    // ...
    // listener ! Unbind
} 
```

On successfully bound, you'll be sent the actor reference of the transport listener actor and the URL which that actor is bound to. If you wish to unbind the listener, just the send an ``Unbind`` message.

```scala
listener ! Unbind
```

### Examples

* [EmbeddedRouterApp](https://github.com/angiolep/akka-wamp/blob/master/examples/router/EmbeddedRouterApp.scala)


## Limitations
TBD

[download-image]: https://api.bintray.com/packages/angiolep/universal/akka-wamp/images/download.svg
[download-url]: https://bintray.com/angiolep/universal/akka-wamp/_latestVersion
