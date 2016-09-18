# Akka Wamp [![Build Status][travis-image]][travis-url] [![Codacy Status][codacy-image]][codacy-url] 
     
Akka Wamp is a WAMP - [Web Application Messaging Protocol](http://wamp-proto.org/) implementation written in [Scala](http://scala-lang.org/) with [Akka](http://akka.io/)

Easy to download as [SBT](http://www.scala-sbt.org/) library dependency.

```scala
libraryDependencies ++= Seq(
  "com.github.angiolep" % "akka-wamp_2.11" % "0.7.0"
)  
```

## Docs [![Docs Status][docs-image]][docs-url] 
Please, [read the docs](http://akka-wamp.readthedocs.io/) for further details.


## Client
Connect a transport, open a session, subscribe a topic, receive events, register a procedure and call it in few lines of Scala!

### Publisher/Subscriber

```scala
object PubSubApp extends App {

  import akka.wamp.client._
  val client = Client()
  
  import scala.concurrent.Future
  import client.executionContext
  
  for {
    session <- client
      .openSession(
        url = "ws://localhost:8080/router",
        subprotocol = "wamp.2.json",
        realm = "akka.wamp.realm",
        roles = Set("subscriber"))
    subscription <- session
      .subscribe(
        topic = "myapp.topic",
        options = Dict()) {
        event =>
          event.payload.map(_.arguments.map(println))
        }
    } yield ()
}
```

### Callee/Caller
Coming soon ...

 
## Router [![Download][download-image]][download-url]
Akka Wamp provides you with a router that can be either embedded into your application or launched as standalone server process.

## Limitations

 * Scala 2.11 only (no older Scala)
 * WebSocket transport only (no raw TCP and no SSL/TLS yet) 
 * Provide WAMP Basic Profile only (no Advanced Profile yet)
 * Provide Publish/Subscribe only (no router RPC yet)
 * Provide JSON serialization only (no MsgPack yet)

## Changelog
Please, read [CHANGELOG.md](CHANGELOG.md)

## Contributing
Please, read [CONTRIBUTING.md](CONTRIBUTING.md)

## Licence 
This software comes with [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)



[travis-image]: https://travis-ci.org/angiolep/akka-wamp.svg?branch=master
[travis-url]: https://travis-ci.org/angiolep/akka-wamp

[codacy-image]: https://api.codacy.com/project/badge/grade/f66d939188b944bbbfacde051a015ca1
[codacy-url]: https://www.codacy.com/app/paolo-angioletti/akka-wamp

[docs-image]: https://readthedocs.org/projects/akka-wamp/badge/?version=v0.7.0
[docs-url]: http://akka-wamp.readthedocs.io/en/v0.7.0/?badge=v0.7.0

[download-image]: https://api.bintray.com/packages/angiolep/universal/akka-wamp/images/download.svg
[download-url]: https://bintray.com/angiolep/universal/akka-wamp/_latestVersion
