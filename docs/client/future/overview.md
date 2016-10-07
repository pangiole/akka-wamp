# Future based API overview

Akka Wamp provides you with an [Akka Future](http://doc.akka.io/docs/akka/current/scala/futures.html) based API, built on top of [Akka Wamp Actor based API](../../client/actor), to let you write your client with a higher level API.

All operations are implemented with direct method calls which return futures you can compose in monadic expressions or in Scala for comprehension.
 

```scala
object FutureBasedClientApp extends App {

  import akka.wamp.client._
  val client = Client()

  implicit val ec = client.executionContext
  
  for {
    session <- client
      .openSession(
        url = "ws://localhost:8080/router",
        subprotocol = "wamp.2.json",
        realm = "akka.wamp.realm",
        roles = Set("subscriber"))
    subscription <- session
      .subscribe(
        topic = "myapp.topic1")(
        event =>
          event.data.map(println)
      )
    publication <- session
      .publish(
        topic = "myapp.topic2",
        ack = false,
        kwdata = Map("name"->"paolo", "age"->40)
      )
  } yield ()
}
```

## Read on

* [Session Handling](../future/session)
* [Publish Subscribe](../future/pubsub)
* [Remote Procedure Call](../future/rpc)
* [Payload Handling](../future/payload)