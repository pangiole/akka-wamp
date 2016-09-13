<a href="https://github.com/angiolep/akka-wamp"><img style="position: absolute; top: 0; right: 0; border: 0;" src="https://camo.githubusercontent.com/38ef81f8aca64bb9a64448d0d70f1308ef5341ab/68747470733a2f2f73332e616d617a6f6e6177732e636f6d2f6769746875622f726962626f6e732f666f726b6d655f72696768745f6461726b626c75655f3132313632312e706e67" alt="Fork me on GitHub" data-canonical-src="https://s3.amazonaws.com/github/ribbons/forkme_right_darkblue_121621.png"></a>

# Akka Wamp [![Build Status][travis-image]][travis-url] [![Codacy Status][codacy-image]][codacy-url] 
     
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
import akka.actor._
import akka.wamp.client._
import akka.wamp.messages._
import akka.wamp.serialization._

object PubSubApp extends App {
  implicit val system = ActorSystem("myapp")
  implicit val ec = system.dispatcher

  val session = Client().connectAndOpenSession()
  
  val handler: EventHandler = { event =>
    event.payload.map(_.arguments.map(println))
  }
  for { 
    ssn <- session
    sub <- ssn.subscribe("myapp.topic")(handler)
  } yield ()

  val payload = Payload(List("paolo", 40, true))
  for {
    ssn <- session
    pub <- ssn.publish("myapp.topic", ack=true, Some(payload))
  } yield ()
}
```

### Callee/Caller
TBD

 
## Router [![Download][download-image]][download-url]
Akka Wamp provides you with a router that can be either embedded into your application or launched as standalone server process.

## Limitations

 * It works with Scala 2.11 (no older Scala)
 * WebSocket transport without SSL/TLS encryption (no raw TCP yet)  
 * Provide WAMP Basic Profile (no Advanced Profile yet)
 * Provide JSON serialization (no MsgPack yet)


## Licence 
This software comes with [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)



[travis-image]: https://travis-ci.org/angiolep/akka-wamp.svg?branch=v0.7.0
[travis-url]: https://travis-ci.org/angiolep/akka-wamp

[codacy-image]: https://api.codacy.com/project/badge/grade/f66d939188b944bbbfacde051a015ca1
[codacy-url]: https://www.codacy.com/app/paolo-angioletti/akka-wamp

[docs-image]: https://readthedocs.org/projects/akka-wamp/badge/?version=latest
[docs-url]: http://akka-wamp.readthedocs.io/en/latest/?badge=latest

[download-image]: https://api.bintray.com/packages/angiolep/universal/akka-wamp/images/download.svg
[download-url]: https://bintray.com/angiolep/universal/akka-wamp/_latestVersion
 