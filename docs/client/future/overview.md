# Future based API

Akka Wamp provides you with an [Akka Future](http://doc.akka.io/docs/akka/current/scala/futures.html) based API, built on top of [Akka Wamp Actor based API](../client/actor), to let you write your client with a high-level API and few lines of Scala!

## For the _impatients_
Let's connect a transport, open a session, subscribe to a topic and receive events:

```scala
object PubSubApp extends App {

  import akka.wamp._
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
        topic = "myapp.topic",
        options = Dict()) {
        event =>
          event.payload.map(_.arguments.map(println))
        }
    } yield ()
}
```

Create the client and invoke its ``openSession()`` method to get a (future of) session. Then yield a (future of) subscription by invoking the ``subscribe()()`` method. That's a curried method which accepts the topic URI in its first parameters list and an ``EventHandler`` handler in its second parameters list. The event handler maps the (option of) payload to get the (future of) ``arguments`` just received.

Please, read on for a deeper explanation and further details.
