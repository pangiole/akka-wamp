
# Embedded
Make your SBT build depend on akka-wamp:

```scala
scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.github.angiolep" %% "akka-wamp" % "0.5.0"
  // ...
)
```

Create both and actor system and materializer, and then create the router actor as follows:

```scala
import akka.actor._
import akka.stream._
import akka.wamp.router._

implicit val system = ActorSystem("wamp")
implicit val mat = ActorMaterializer()

val router = system.actorOf(Router.props(), "router")
IO(Wamp) ! Bind(router)
```

It automatically binds on a server socket by reading the following Akka configuration

 - ``akka.wamp.router``  
   
    - ``protocol``  
      The protocol the router uses as transport (default is ``ws`` WebSocket)

    - ``subprotocol``  
       The subprotocol the router uses when transport is WebSocket (default is ``wamp.2.json``)

    - ``iface``  
      The network interface the router binds to (default is ``127.0.0.1``)

    - ``port``  
      The port number the router binds to (default is ``8080``)

    - ``path``  
      The path the router expects WebSocket connection requests (default is ``/ws``)


> NOTE: the Akka Wamp Router, by default, expects HTTP Upgrade to WebSocket requests addressed to ``http://127.0.0.1:8080/ws``



# Standalone
Download and launch the router as standalone application:

```bash
curl https://dl.bintray.com/angiolep/universal/akka-wamp-0.5.0.tgz
tar xvfz akka-wamp-0.5.0.tar.gz
cd akka-wamp-0.5.0
./bin/akka-wamp -Dakka.loglevel=DEBUG
```

You can ovveride the default setting by passing Java system properties on the command line.
