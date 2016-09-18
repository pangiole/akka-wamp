<a href="https://github.com/angiolep/akka-wamp"><img style="position: absolute; top: 0; right: 0; border: 0;" src="https://camo.githubusercontent.com/38ef81f8aca64bb9a64448d0d70f1308ef5341ab/68747470733a2f2f73332e616d617a6f6e6177732e636f6d2f6769746875622f726962626f6e732f666f726b6d655f72696768745f6461726b626c75655f3132313632312e706e67" alt="Fork me on GitHub" data-canonical-src="https://s3.amazonaws.com/github/ribbons/forkme_right_darkblue_121621.png"></a>

# Akka Wamp 
     
Akka Wamp is a WAMP - [Web Application Messaging Protocol](http://wamp-proto.org/) implementation written in [Scala](http://scala-lang.org/) with [Akka](http://akka.io/)

Easy to download as [SBT](http://www.scala-sbt.org/) library dependency.

```scala
libraryDependencies ++= Seq(
  "com.github.angiolep" % "akka-wamp_2.11" % "0.7.0"
)  
```

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
TBD

 
## Router
Akka Wamp provides you with a router that can be either embedded into your application or launched as standalone server process.

## Limitations

 * Scala 2.11 only (no older Scala)
 * WebSocket transport only (no raw TCP and no SSL/TLS yet) 
 * Provide WAMP Basic Profile only (no Advanced Profile yet)
 * Provide Publish/Subscribe only (no router RPC yet)
 * Provide JSON serialization only (no MsgPack yet)

 
## Licence 
This software comes with [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)

