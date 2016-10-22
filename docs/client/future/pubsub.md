# Publish and Subscribe

```scala
object PubSubApp extends App {

  import akka.wamp.client._
  val client = Client()

  implicit val ec = client.executionContext

  val publication = 
    for {
      session <- client
        .openSession(
          url = "ws://localhost:8080/ws",
          subprotocol = "wamp.2.json",
          realm = "default.realm",
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

The WAMP protocol defines a Publish and Subscribe (PubSub) communication pattern where a client, the subscriber, informs the broker router that it wants to receive events on a topic (i.e., it subscribes to a topic). Another client, a publisher, can then publish events to this topic, and the broker router distributes events to all subscriber.

```
  ,---------.          ,------.             ,----------.
  |Publisher|          |Broker|             |Subscriber|
  `----+----'          `--+---'             `----+-----'
       |                  |                      |
       |                  |            SUBSCRIBE |
       |                  <<---------------------|
       |                  |                      |
       |                  | SUBSCRIBED           |
       |                  |--------------------->>
       |                  |                      |
       |                  |                      |
       | PUBLISH          |                      |
       |----------------->>                      |
       |                  |                      |
       |        PUBLISHED |                      |
       <<-----------------|                      |
       |                  |                      |
       |                  |  EVENT               |
       |                  |--------------------->>
       |                  |                      |
  ,----+----.          ,--+---.             ,----+-----.
  |Publisher|          |Broker|             |Subscriber|
  `---------'          `------'             `----------'
```

## Subscribe topics
Once you got a subscriber session as documented in the [Session Handling](../future/session) section, you can finally subscribe to a topic with its event handler:

```scala
import akka.wamp._
import akka.wamp.message._

// implicit val executionContext = ...
// val session = ... 

val subscription: Future[Subscription] = session.flatMap(
  _.subscribe(
    topic = "myapp.topic") { 
    event =>
      log.info(s"${event.publicationId}")
      event.data.map(println)
    })
```

A (future of) session can be mapped to a (future of) subscription by just invoking the ``subscribe`` method. It is a curried method with two parameter lists.

```scala
// as defined by Akka Wamp

def subscribe(topic: Uri)(handler: EventHandler)
```

The first parameter list accepts ``topic`` as documented for the [``Subscribe``](../../messages#Subscribe) message constructor. The second parameter list accepts a callback function of type ``EventHandler`` which gets invoked to process each event from the topic. 

### Event handlers

```scala
// as defined by Akka Wamp
type EventHandler = (Event) => Unit
```

The event handler is a function with side-effects that transforms an event to ``unit`` (like ``void`` for Java). The event comes with an input payload whose content can be lazily parsed as input data. 

```scala
// your event handler
val handler: EventHandler = { event =>
  event.data.map { data =>
    println(data)
  }
}
```

Receiving application data is documented in the [Payload Handling](./payload) section.  

### Multiple handlers

```scala
val handler1: EventHandler = { event =>
  event.data.map { data =>
    println(s"(1) <-- $data")
  }
}
val handler2: EventHandler = { event =>
  event.data.map { data =>
    println(s"(2) <-- $data")
  }
}

// let's subscribe them to the same topic! 

val subscription1 = session.flatMap(
  _.subscribe(
    topic = "myapp.topic")(
    handler1))

val subscription2 = session.flatMap(
  _.subscribe(
    topic = "myapp.topic")(
    handler2))
```

You can subscribe many times to the same topic passing the same or different event handlers. As per WAMP protocol specification, the correspondent subscriptions held by the router will share the same subscription identifier. Therefore, your Akka Wamp client subscriber will: 

* add your event handlers in the same set linked to the same subscription identifier,
* invoke all of them anytime an event with that subscription identifier is received.


### Recover
You can either recover or _"give up"_ when the (future of) subscription fails. To recover from failures (such as ``SessionException`` thrown when session turns out to be closed) you can compose ``recoverWith`` to attempt another session opening (maybe to a fallback realm and/or to a fallback topic):

```scala
val subscription = session.flatMap(
  _.subscribe("myapp.topic")(handler)
  .recoverWith { 
    case ex: SessionException => session.flatMap(
      _.subscribe("myapp.topic.heartbeat")(handler)
  }
```

As last resort, instead of recovering, you could decide to _"give up"_ a callback function ``onFailure`` that just prints a log message:

```scala
session.onFailure {
  case ex: Throwable => 
    log.error(ex.getMessage, ex)
}
```



## Unsubscribe topics

```scala
import akka.wamp.messages._

val subscription: Future[Subscription] = ...

val unsubscribed: Future[Unsubscribed] = subscription.flatMap(
  _.unsubscribe()
)
```

Just call the ``unsubscribe()`` method on the subscription you want to terminate.
 

### Multiple handlers
TBD


## Publish events
```scala
import akka.Done
import akka.wamp.serialization._

val publication: Future[Either[Done, Publication]] = session.flatMap(
  _.publish(
    topic = "myapp.topic",
    payload = Payload("paolo", 40, true),
    ack = true
  ))
```

A (future of) session can be mapped to a (future of) either done or publication by just invoking the ``publish`` method which accepts ``topic``, ``ack`` and a ``payload`` arguments as documented for the [``Publish``](../../messages#Publish) message constructor. Sending arguments is documented in the [Payload Handling](./payload) section.  


### Acknowledge

Note that if you leave ``ack`` switched off (as by default) then Akka Wamp will not expect to receive the [``Published``](../../messages#Publish) message back from the router and the (future of either of) publication or done immediately completes with (left of) ``Done``. Otherwise, if you switch ``ack`` on then the (future of either of) publication or done later completes with (right of) ``Publication`` (if no exception were thrown).

You could pass a callback ``onSuccess`` to better understand what really happens:

```scala
// ack = true
publication.onSuccess {
  case Success(Left(Done)) =>
    println(s"Publication done") 
}

// ack = false
publication.onSuccess {
  case Success(Right(p)) =>
    println(s"Published with ${p.publicationId}")
}
```


### Recover

You can either recover or _"give up"_ when the (future of) publication fails. To recover from failures (such as ``SessionException`` when session turns out to be closed as you try to publish) you can compose ``recoverWith``  to attempt another session opening (maybe to a fallback realm and to a fallback topic):

```scala
val publication = session1.flatMap(_.publish("myapp.topic.ticking")
  .recoverWith { 
    case ex: SessionException =>
      for {
        session2 <- client.openSession()
        publication2 <- session2.publish("myapp.topic.heartbeat")
      }
      yield publication2
  }
```

As last resort, instead of recovering, you could decide to _"give up"_ a callback function ``onFailure`` that just prints a log message:

```scala
publication.onFailure {
  case ex: Throwable => 
    log.error(ex.getMessage, ex)
}
```

