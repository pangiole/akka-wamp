Akka Wamp provides you with a router that can be either embedded into your application or launched as standalone server process.

## Embedded
Make your SBT build depend on akka-wamp:

```scala
scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.github.angiolep" %% "akka-wamp" % "0.9.0"
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
curl https://dl.bintray.com/angiolep/universal/akka-wamp-0.9.0.tgz
tar xvfz akka-wamp-0.9.0.tar.gz
cd akka-wamp-0.9.0
./bin/akka-wamp -Dakka.loglevel=DEBUG
```

## Configuration
Either the embedded or the standalone router can be configured by applying the following configuration:
 
```bash
akka {
  wamp {
    router {
      # The TCP interface to bind to
      #
      iface = "127.0.0.1"
      
      # The TCP port number (between 0 and 65536) to bind to
      #
      port = 8080
      
      # The URL path incoming HTTP Upgrade request are
      # expected to be addressed to
      #
      path = "router"

      # The boolean switch to validate against strict URIs 
      # rather than loose URIs
      #
      validate-strict-uris = false

      # The boolean switch to NOT automatically create 
      # realms if they don't exist yet.
      #
      abort-unknown-realms = false

      # The boolean switch to disconnect those peers that send 
      # offending messages (e.g. not deserializable or causing
      # session failures)
      #
      # By default, offending messages are just dropped and 
      # the router resumes processing next incoming messages
      #
      disconnect-offending-peers = false
    }
  }
}
```
      
Above settings can be overridden by

 * providing a TypeSafe Config ``application.conf`` file right in the classpath,
 * or passing Java system properties (e.g. ``-Dakka.wamp.router.port=7070``) to the Java interpreter on the command line


[download-image]: https://api.bintray.com/packages/angiolep/universal/akka-wamp/images/download.svg
[download-url]: https://bintray.com/angiolep/universal/akka-wamp/_latestVersion
