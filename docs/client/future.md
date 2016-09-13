If you wish your client to be written with a high-level API, and you need to implement no more than practical scenarios, then Akka Wamp provides you with an [Akka Future](http://doc.akka.io/docs/akka/current/scala/futures.html) based API.

It requires you to know what futures are and how to compose them in _monadic_ expressions. You'll be rewarded with succinct and elegant Scala code by just composing asynchronous functions that return transports, sessions, subscriptions, registrations, _"et cetera"_. 

## For the _impatients_
Let's connect a transport, open a session, subscribe a topic and receive events in few lines of Scala!

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

``Client()`` is the entry point object. Invoke its ``connectAndOpenSession()`` method to get a (future of) session and then yield a (future of) subscription  by invoking the ``subscribe()()`` method. That's a curried method which accepts the topic URI in its first parameters list and an ``EventHandler`` handler in its second parameters list. The event handler maps the (option of) payload to get the (future of) ``arguments`` published by some remote client in a way it __will never ever block__ :-)

Please, read on for a deeper explanation and further details.

## Establish connections

```scala
import akka.actor._
import akka.wamp.client._
import scala.concurrent._

implicit val system = ActorSystem("myapp")
implicit val ec = system.dispatcher

val client = Client()
val connection: Future[Connection] = client.connect(
  url = "ws://127.0.0.1:8080/ws",
  subprotocol = "wamp.2.json"
)
```
Since Akka Wamp is built on Akka, as any other applications built on Akka, it needs the following implicit values in scope:

* the Akka ActorSystem
* a ``scala.concurrent.ExecutionContext``

The execution context could be the actor system dispatcher (as in the above example) or a different one you might desire to create and configure on purpose.


The ``Client`` object is the entry point of the API and it has to be created before any other objects. It takes care of the asynchronous non-blocking communication with the router by exchanging WAMP messages over a transport (for example WebSocket or RawTCP) and it has been written using the low level [Actor based API](/client/actor).

The ``Client`` object provides the ``connect()`` method which accepts ``url`` and ``subprotocol`` arguments as documented for the [``Connect``](../../messages#Connect) message constructor. It returns a (future of) connection that can be composed in monadic expressions.


You can either recover or _"give up"_ when the (future of) connection fails. To recover from failures (such as ``ConnectionException`` when router doesn't accept) you can compose a ``recoverWith`` function to attempt another connection (maybe to a fallback router):

```scala
val connection = client.connect()
  .recoverWith { 
    case ex: ConnectionException =>
      client.connect(
        url = "ws://fallback.host.net:9999/ws",
        subprotocol = "wamp.2.msgpack"
      )
  }
```

Instead of recovering, you could decide to _"give up"_ by a callback function ``onFailure`` that just prints a log message:

```scala
connection.onFailure {
  case ex: Throwable => 
    system.log.error(ex.getMessage, ex)
}
```

## Open sessions

```scala
val session: Future[Session] = connection.flatMap(
  _.openSession(
    realm = "myapp.realm",
    roles = Set(Roles.subscriber)
  ))
```

A (future of) connection can be mapped to a (future of) session by just invoking the ``openSession()`` method which accepts ``realm`` and ``roles`` arguments as documented for the [``Hello``](../../messages#Hello) message constructor.


You can also collapse connection establishment and session opening in one single concise statement as follows:

```scala
val session: Future[Session] = client
  .connectAndOpenSession(
    url = "ws://some.host.net:8080/ws",
    subprotocol = "wamp.2.json",
    realm = "myapp.realm",
    roles = Set(Roles.subscriber)
  )
```

The ``connectAndOpenSession()`` method accepts all of the ``url``, ``subprotocol``, ``realm`` and ``details`` arguments as mentioned above.


You can either recover or _"give up"_ when the (future of) session fails. To recover from failures (such as ``AbortException`` when router doesn't attach to a realm) you can compose a ``recoverWith`` function to attempt another session opening (maybe to a fallback realm):

```scala
val session = connection.flatMap(
  _.openSession("myapp.realm"))
  .recoverWith { 
    case ex: AbortException =>
      connection.flatMap(
        _.openSession(
          realm = "fallback.realm"
          // roles = Set(Roles.all)
       ))
  }
```

Instead of recovering, you could decide to _"give up"_ by a callback function ``onFailure`` that just prints a log message:

```scala
session.onFailure {
  case ex: Throwable => 
    system.log.error(ex.getMessage, ex)
}
```

## Subscribe topics
```scala
import akka.wamp._
import akka.wamp.messages._

val handler: EventHandler = { event => 
  event.payload.map { payload =>
    payload.arguments.map { arguments =>
      println(arguments)
    }
  }
}

val subscription: Future[Subscribed] = session.flatMap(
  _.subscribe(
    topic = "myapp.topic.people",
    options = Dict()
)(handler))
```

A (future of) session can be mapped to a (future of) subscription by just invoking the ``subscribe`` method. It is a curried method with two parameter lists.

```scala
def subscribe(topic: String, options: Dict)(handler: EventHandler)
```

The first parameter list accepts ``topic`` and ``options`` arguments as documented for the [``Subscribe``](../../messages#Subscribe) message constructor. The second parameter list accept a callback handler function of type ``EventHandler`` which gets invoked to process each event from the topic. The ``event`` object provides a (option of) ``payload`` which in turn provides both (future of) ``arguments`` and ``argumentsKw``.

You can either recover or _"give up"_ when the (future of) subscription fails. To recover from failures (such as ``SessionException`` when session turns to be closed as you try to subscribe) you can compose a ``recoverWith`` function to attempt another session opening (maybe to a fallback realm and/or to a fallback topic):

```scala
val subscription = session.flatMap(_.subscribe("myapp.topic.ticking")(handler)
  .recoverWith { 
    case ex: SessionException =>
      for {
        session2 <- connection.flatMap(_.openSession("myapp.realm"))
        subscription2 <- session2.subscribe("myapp.topic.heartbeat")(handler)
      }
      yield subscription2
  }
```

Instead of recovering, you could decide to _"give up"_ by a callback function ``onFailure`` that just prints a log message:

```scala
session.onFailure {
  case ex: Throwable => 
    system.log.error(ex.getMessage, ex)
}
```


## Publish events
```scala
import akka.Done
import akka.wamp.serialization._

val publication: Future[Either[Done, Published]] = session.flatMap(
  _.publish(
    topic = "myapp.topic.people",
    acknowledge = true,
    payload = Some(Payload(List("paolo", 40, true)))
  ))
```

A (future of) session can be mapped to a (future of) either done or published by just invoking the ``publish`` method which accepts ``topic``, ``acknowledge`` and (option of) ``payload`` arguments as documented for the [``Publish``](../../messages#Publish) message constructor.


Note that if you leave the ``acknowledge`` boolean switched off (as by default) then Akka Wamp will not expect to receive the [``Published``](../../messages#Publish) message back from the router and publication immediately completes with (left of) ``Done``. Otherwise, if you switch the ``acknowledge`` boolean flag on then publication later completes with ``Published`` (if no exception were thrown).

You could pass a callback ``onSuccess`` to better understand what happens:

```scala
// acknowledge = true
publication.onSuccess {
  case Success(Left(Done)) =>
    println(s"Publication done") 
}

// acknowledge = false
publication.onSuccess {
  case Success(Right(p)) =>
    println(s"Published with ${p.publicationId}")
}
```


You can either recover or _"give up"_ when the (future of) publication fails. To recover from failures (such as ``SessionException`` when session turns to be closed as you try to publish) you can compose a ``recoverWith`` function to attempt another session opening (maybe to a fallback realm and to a fallback topic):

```scala
val publication = session.flatMap(_.publish("myapp.topic.ticking")
  .recoverWith { 
    case ex: SessionException =>
      for {
        session2 <- connection.flatMap(_.openSession("myapp.realm"))
        publication2 <- session2.publish("myapp.topic.heartbeat")
      }
      yield publication2
  }
```

Instead of recovering, you could decide to _"give up"_ by a callback function ``onFailure`` that just prints a log message:

```scala
publication.onFailure {
  case ex: Throwable => 
    system.log.error(ex.getMessage, ex)
}
```


## For-comprehension
Above code examples were deliberately verbose because this documentation is aimed to give you as much details as possible. You can make your code more concise if

* you don't need to hold the connection reference,
* you don't need to insert recovery code at each stage,
* you are happy with default values 

then you might prefer to compose your stages on the _"happy path"_ via Scala for-comprehension statements:

```scala
val subscription = for {
  session <- client.connectAndOpenSession(
    url = "ws://some.host.net:8080/ws",
    realm = "myapp.realm"
  )
  subscription <- session.subscribe(
    topic = "myapp.topic.people",
    options = Dict()
  )(handler)
}
yield subscription
```

Bear in mind that if any exception occurs at any stage then it makes _fail fast_ the entire for-comprehension construct and the (future of) subscription immediately completes with failure. Therefore, you might need something like the following ``onComplete`` callback to pattern match the publication completion:

```scala
subscription.onComplete {
  case Success(s) =>
    println(s"Subscribed with ${s.subscriptionId}")
  case Failure(ex) =>
    system.log.error(ex, ex.getMessage)
    for (t <- system.terminate()) System.exit(-1)
}
```

A similar for-comprehension construct can be written for your client publisher:

```scala
val publication = for {
  session <- client.connectAndOpenSession(
    url = "ws://some.host.net:8080/ws",
    realm = "myapp.realm"
  )
  publication <- session.publish(
    topic = "myapp.topic.people",
    // acknowledge = false,
    payload = Some(Payload("paolo", 40, true))
  )
}
yield publication
```

