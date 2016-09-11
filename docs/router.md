Akka Wamp provides you with a router that can be either embedded into your application or launched as standalone server process.

## Embedded
Make your SBT build depend on akka-wamp:

```scala
scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.github.angiolep" %% "akka-wamp" % "0.7.0"
  // ...
)
```

Create the Akka ``ActorSystem`` and the Akka Wamp ``Router`` actor as follows:

```scala
import akka.actor._
import akka.wamp.router._

implicit val system = ActorSystem("wamp")
val router = system.actorOf(Router.props(), "router")
```

Then send a ``Bind`` message to the ``IO(Wamp)`` manager

```scala
IO(Wamp) ! Bind(router)
```

Done ;-)

## Standalone [![Download][download-image]][download-url]
Download and launch the router as standalone application:

```bash
curl https://dl.bintray.com/angiolep/universal/akka-wamp-0.7.0.tgz
tar xvfz akka-wamp-0.7.0.tar.gz
cd akka-wamp-0.7.0
./bin/akka-wamp -Dakka.loglevel=DEBUG
```

## Settings
Either the embedded or the standalone router can be configured by applying the following settings:


 - ``akka.wamp.serialization``  
   
    - ``validate-strict-uris``  
      The boolean switch (default is false) to validate against strict URIs rather than loose URIs 
 
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
      
    - ``abort-unknown-realms``  
      The boolean switch (default is false) to NOT automatically create realms if they don't exist yet

Above settings can be overridden by

 * providing a TypeSafe Config ``application.conf`` file right in the classpath,
 * or passing Java system properties (e.g. ``-Dakka.wamp.router.port=7070``) to the Java interpreter on the command line


[download-image]: https://api.bintray.com/packages/angiolep/universal/akka-wamp/images/download.svg
[download-url]: https://bintray.com/angiolep/universal/akka-wamp/_latestVersion
