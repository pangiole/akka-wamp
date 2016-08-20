# Future based API
If you wish your client to be written with a high-level API, and you need to implement no more than practical scenarios, then Akka Wamp provides you with an [Akka Future](http://doc.akka.io/docs/akka/current/scala/futures.html) based API.

It requires you to know what futures are and how to compose them in _monadic_ expressions. You'll be rewarded with succinct and elegant Scala code by just composing asynchronous functions that return transports, sessions, subscriptions, registrations, _"et cetera"_. 

## For the _impatients_
Let's connect a transport, open a session, subscribe a topic and receive events in few lines of Scala!

```scala
for (session <- Client().connectAndHello("ws://host:8080/ws"))
  yield session.subscribe("myapp.topic") { event =>
    event.payload.map { p =>
      system.log.info(payload.arguments.toString)
    }
  }
```

``Client()`` is the entry point object. Invoke its ``connectAndOpen()`` method to get a (future of) session and then yield a (future of) subscription  by invoking the ``subscribe()()`` method. That's a curried method which accepts the topic URI in its first parameters list and an ``EventHandler`` handler in its second parameters list. The event handler maps the (option of) payload to get the ``arguments`` sent by the remote publisher ... and it __will never ever block__, of course ;-)

Please, read on for a deeper explanation and further details.

### Prepare Akka scope
```scala
import akka.wamp._
import akka.wamp.client._
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import scala.concurrent._

implicit val system = ActorSystem()
implicit val materializer = ActorMaterializer()
implicit val executionContext = system.dispatcher
```
Since Akka Wamp is built on Akka, as any other applications built on Akka, it needs the following implicit values in scope:

* the Akka ActorSystem
* the Akka ActorMaterializer
* a ``scala.concurrent.ExecutionContext``

The execution context could be the actor system dispatcher (as in the above example) or a completely different one you might desire to create and configure on purpose.

## Connect transport
```scala
val client = Client()
val transport: Future[Transport] = client.
  connect(
    url = "ws://127.0.0.1:8080/ws",
    subprotocol = "wamp.2.json"
  )
```

The ``Client`` object is the entry point of the API and it has to be created before any other objects. 

The ``Client`` object takes care of the asynchronous non-blocking communication with the router by exchanging WAMP messages over an underlying transport (for example WebSocket or RawTCP) and it has been written using the low level [Akka Wamp Actor based API](/client/actor).

The ``Client`` object provides the ``connect()`` method which accepts ``url`` and ``subprotocol`` arguments as documented for the [``Connect``](../../messages#Connect) message constructor. It returns a (future of) transport that can be composed in monadic expressions.

### Handle exceptions
You can either recover or _"give up"_ in case the (future of) transport fails.

To recover from failures (such as ``ConnectionException`` when router doesn't accept) you can compose a ``recoverWith`` function to attempt another connection (maybe to a fallback router):

```scala
val transport = client.connect()
  .recoverWith { case ex: ConnectionException =>
    system.log.warn(s"Recovering from $ex")
    client.connect(
      url = "ws://fallback.host.net:9999/ws",
      subprotocol = "wamp.2.msgpack"
    )
  }
```

Instead, when there's no hope but you want to _"give up"_, then pass a callback ``onFailure`` to print a log message:

```scala
transport.onFailure {
  case ex: Throwable => 
    system.log.error(ex.getMessage, ex)
}
```

## Open session
```scala
val session: Future[Session] = transport.flatMap(
  _.open(
    realm = "myapp.realm",
    roles = Set("subscriber")
  ))
```

A (future of) transport can be mapped to a (future of) session by just invoking the ``open()`` method which accepts ``realm`` and ``roles`` arguments as documented for the [``Hello``](../../messages#Hello) message constructor.

You might desire to collapse transport connection and session opening in one concise statement as follows:

```scala
val session: Future[Session] = 
  client.connectAndOpen(
    url = "ws://some.host.net:8080/ws",
    subprotocol = "wamp.2.json",
    realm = "myapp.realm",
    roles = Set("subscriber")
  )
```

The ``connectAndOpen()`` method accepts all of the ``url``, ``subprotocol``, ``realm`` and ``details`` arguments mentioned above.

### Handle exceptions
You can either recover or _"give up"_ in case the (future of) session fails.

To recover from failures (such as ``AbortException`` when router doesn't attach to a realm) you can compose a ``recoverWith`` function to attempt another session opening (maybe to a fallback realm):

```scala
val session = transport.flatMap(_.open("myapp.realm"))
  .recoverWith { case ex: AbortException =>
    system.log.warn(s"Recovering from $ex")
    transport.flatMap(
      _.open(
        realm = "fallback.realm"
        // roles = Set("subscriber", "publisher")
     ))
  }
```

Instead, when there's no hope but you want to _"give up"_, then pass a callback ``onFailure`` to print a log message:

```scala
session.onFailure {
  case ex: Throwable => 
    system.log.error(ex.getMessage, ex)
}
```

## Subscribe topics
```scala
val handler: EventHandler = { event =>
 for (payload <- event.payload)
   system.log.info(payload.arguments)
}

val subscription: Future[Subscribed] = session.flatMap(
  _.subscribe(
    topic = "myapp.topic.people"
    //options = Dict()
  )(handler)
```

A (future of) session can be mapped to a (future of) subscription by just invoking the ``subscribe`` method. It is a curried method with two parameter lists.

```scala
def subscribe(topic: String, options: Dict)(handler: EventHandler)
```

The first parameter list accepts ``topic`` and ``options`` arguments as documented for the [``Subscribe``](../../messages#Subscribe) message constructor. The second parameter list accept a callback handler function of type ``EventHandler`` which gets invoked to process each event from the topic.

### Handle exceptions
You can either recover or _"give up"_ in case the (future of) subscription fails.

To recover from failures (such as ``SessionException`` when session turns to be closed as you try to subscribe) you can compose a ``recoverWith`` function to attempt another session opening (maybe to a fallback realm and to a fallback topic):

```scala
val subscription = session.flatMap(_.subscribe("myapp.topic.ticking")(handler)
  .recoverWith { case ex: SessionException =>
    system.log.warn(s"Recovering from $ex")
    for {
      session2 <- transport.open("myapp.realm")
      subscription2 <- session2.subscribe("myapp.topic.heartbeat")(handler)
    }
    yield subscription2
  }
```

Instead, when there's no hope but you want to _"give up"_, then pass a callback ``onFailure`` to print a log message:

```scala
session.onFailure {
  case ex: Throwable => 
    system.log.error(ex.getMessage, ex)
}
```


## Publish events
```scala
val publication: Future[Either[Done, Published]] = session.flatMap(
  _.publish(
    topic = "myapp.topic.people",
    acknowledge = true,
    payload = Some(Payload("paolo", 40, true))
  ))
```

A (future of) session can be mapped to a (future of) either done or published by just invoking the ``publish`` method which accepts ``topic``, ``acknowledge`` and (option of) ``payload`` arguments as documented for the [``Publish``](../../messages#Publish) message constructor.


Note that if you leave the ``acknowledge`` boolean switched off (as by default) then Akka Wamp will not expect to receive the [``Published``](../../messages#Publish) message back from the router and the publication future immediately completes with (left of) ``Done``. Otherwise, if you switch the ``acknowledge`` boolean flag on then the publication future could complete with ``Published`` (if no exception were thrown).

You'd be better off passing a callback to ``onSuccess``:

```scala
// acknowledge = true
publication.onSuccess {
  case Success(Left(Done)) =>
    system.log.info(s"Publication done") 
}

// acknowledge = false
publication.onSuccess {
  case Success(Right(p)) =>
    system.log.info(s"Published with ${p.publicationId}")
}
```

### Handle exceptions
You can either recover or _"give up"_ in case the (future of) publication fails.

To recover from failures (such as ``SessionException`` when session turns to be closed as you try to publish) you can compose a ``recoverWith`` function to attempt another session opening (maybe to a fallback realm and to a fallback topic):

```scala
val publication = session.flatMap(_.publish("myapp.topic.ticking")
  .recoverWith { case ex: SessionException =>
    system.log.warn(s"Recovering from $ex")
    for {
      session2 <- tranport.open("myapp.realm")
      publication2 <- session2.publish("myapp.topic.heartbeat")
    }
    yield publication2
  }
```

Instead, when there's no hope but you want to _"give up"_, then pass a callback ``onFailure`` to print log messages:

```scala
session.onFailure {
  case ex: Throwable => 
    system.log.error(ex.getMessage, ex)
}
```


## For-comprehension
Above code examples were deliberately verbose because this documentation is aimed to give you as much details as possible. You can make your code more concise, of course, if 

* you don't mind to hold the transport reference
* you are fine with default values 
* you don't have any useful recovery code to insert at each stage

then you might prefer to compose your stages on the _"happy path"_ via Scala for-comprehension statements:

```scala
val subscription = for {
  session <- client.connectAndOpen(
    url = "ws://some.host.net:8080/ws",
    realm = "myapp.realm"
  )
  subscription <- session.subscribe(
    topic = "myapp.topic.people"
  ){
    event: Event =>
      for (payload <- event.payload)
        system.log.info(payload.arguments)
  }
  
} yield subscription
```

Bear in mind that if any exception occurs at any stage then it makes _fail fast_ the entire for-comprehension construct and the (future of) subscription immediately completes with failure. Therefore, you might need something like the following ``onComplete`` callback to pattern match the publication future completion:

```scala
subscription.onComplete {
  case Success(s) =>
    system.log.info(s"Subscribed with ${s.subscriptionId}")
  case Failure(ex) =>
    system.log.error(ex, ex.getMessage)
    for (t <- system.terminate()) System.exit(-1)
}
```

A similar for-comprehension construct can be written for your client publisher:

```scala
val publication = for {
  session <- client.connectAndOpen(
    url = "ws://some.host.net:8080/ws",
    realm = "myapp.realm"
  )
  publication <- session.publish(
    topic = "myapp.topic.people",
    // acknowledge = false,
    payload = Some(Payload("paolo", 40, true))
  )
} yield publication
```

