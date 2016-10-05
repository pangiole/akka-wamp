# Router
Akka Wamp provides you with a router that can be either launched as standalone server process or embedded into your application.

It provides:

* Both __broker__ (for publish/subscribe) and __dealer__ (for routed remote procedure calls) roles.
* JSON serialization



## Standalone 
[![Download][download-image]][download-url]

Download the latest router version, extract it, configure it and launch it as standalone application:

```bash
curl https://dl.bintray.com/angiolep/universal/akka-wamp-0.10.0.tgz
tar xvfz akka-wamp-0.10.0.tar.gz
cd akka-wamp-0.10.0
vim ./conf/application.conf
./bin/akka-wamp -Dakka.loglevel=DEBUG
```


<a id="configuration"></a>


## Configuration
Either the embedded or the standalone router can be configured by applying the following configuration:
 
```bash
akka {
  wamp {
    router {
      # Underlying transport can be one of the followings:
      #
      # - tcp
      #     Raw TCP
      # - tsl
      #     Transport Secure Layer
      # - ws    
      #     WebSocket 
      # - wss
      #     WebSocket over SSL/TLS
      #
      transport = "ws"
            
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


### SSL/TLS support
TBD

## Embedded
Make your build depend on the latest version of akka-wamp: 

```scala
scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.github.angiolep" %% "akka-wamp" % "0.10.0"
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
To bind a transport, just send a ``Bind`` command to the ``IO(Wamp)`` extension manager:

```scala
val manager = IO(Wamp)
manager ! Bind(router)
```

The manager will spawn a new transport listener for the given the router binding it as per above configuration. Your application actor, the binder, will be notified by the manager about the outcome of the command:

```scala
override def receive: Receive = {
  case signal @ Wamp.CommandFailed(cmd, ex) =>
    log.info(s"$cmd failed because of $ex")

  case signal @ Wamp.Bound(listener, url) =>
    log.info(s"Bound succeeded with $url")
    // ...
    listener ! Wamp.Unbind
} 
```

On successfully bound, you'll be sent the actor reference of the transport listener actor and the URL which that actor is bound to. If you wish to unbind the listener, just the send an ``Unbind`` message.

```scala
listener ! Wamp.Unbind
```




## Limitations
TBD

[download-image]: https://api.bintray.com/packages/angiolep/universal/akka-wamp/images/download.svg
[download-url]: https://bintray.com/angiolep/universal/akka-wamp/_latestVersion
