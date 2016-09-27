# Publish and Subscribe
TBD

## Subscribe topics
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
      for(p <- event.payload; args <- p.arguments) 
        println(args)
    })
```

A (future of) session can be mapped to a (future of) subscription by just invoking the ``subscribe`` method. It is a curried method with two parameter lists.

```scala
def subscribe(topic: Uri)(handler: EventHandler)
```

The first parameter list accepts ``topic`` as documented for the [``Subscribe``](../../messages#Subscribe) message constructor. The second parameter list accept a callback function of type ``EventHandler`` which gets invoked to process each event from the topic. 

```scala
type EventHandler = (Event) => Unit
```

The event handler is a function that transforms an event object into the unit value (roughly like ``void``). The event object comes with an (option of) input ``payload`` bearing application arguments. Receiving arguments is documented in the [Payload Handling](./payload) section.  

### Multiple handlers
```
val handler1: EventHandler = { event =>
  for(p <- event.payload; args <- p.arguments)
    println(s"(1) <-- $args")
}

val subscription1 = session.flatMap(
  _.subscribe(
    topic = "myapp.topic")(
    handler1))
   
// let's subscribe to the same topic ... again! 
 
val handler2: EventHandler = { event =>
  for(p <- event.payload; args <- p.arguments)
    println(s"(2) <-- $args")     
}
   
val subscription2 = session.flatMap(
  _.subscribe(
    topic = "myapp.topic")(
    handler2))
```

You can subscribe many times to the same topic passing the same or different event handlers. As per protocol specification, the correspondent subscriptions held by the router will share the same subscription identifier. Therefore, the Akka Wamp client subscriber: 

* adds your event handlers in a set linked to the same subscription identifier,
* invokes all of them when an EVENT with that subscription identifier is received.


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

val unsubscribed: Future[Unsubscribed] = session.flatMap(
    _.unsubscribe("myapp.topic")
  )
```

### Multiple handlers
TBD


## Publish events
```scala
import akka.Done
import akka.wamp.serialization._

val publication: Future[Either[Done, Publication]] = session.flatMap(
  _.publish(
    topic = "myapp.topic",
    payload = Some(Payload("paolo", 40, true)),
    ack = true
  ))
```

A (future of) session can be mapped to a (future of) either done or publication by just invoking the ``publish`` method which accepts ``topic``, ``ack`` and (option of) ``payload`` arguments as documented for the [``Publish``](../../messages#Publish) message constructor.



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

