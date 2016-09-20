# Future based API

Akka Wamp provides you with an [Akka Future](http://doc.akka.io/docs/akka/current/scala/futures.html) based API, built on top of [Akka Wamp Actor based API](../client/actor), to let you write your client with a high-level API and few lines of Scala!

## For the _impatients_
Let's connect a transport, open a session, subscribe to a topic and receive events:

```scala
object PubSubApp extends App {

  import akka.wamp._
  import akka.wamp.client._
  
  val client = Client()
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

Create the client and invoke its ``openSession()`` method to get a (future of) session. Then yield a (future of) subscription by invoking the ``subscribe()()`` method. That's a curried method which accepts the topic URI in its first parameters list and an ``EventHandler`` handler in its second parameters list. The event handler maps the (option of) payload to get the (future of) ``arguments`` just received.

Please, read on for a deeper explanation and further details.

## Create the client

```scala
import akka.wamp.client._
val client = Client("myapp")
```

The ``Client`` object is the entry point of this API. Create and name it by invoking its companion factory method which internally creates an Akka [ActorSystem](http://doc.akka.io/docs/akka/2.4.10/general/actor-systems.html) named after it. 

> __Note__  
> The actor system is a heavyweight structure that allocates 1..n threads, so create _one client per logical application_.
 
The client takes care of all the asynchronous communication with the router by exchanging WAMP messages over a connected transport (by default WebSocket). All of the operation provided by client, connection and sessions objects always return futures. In order to execute callbacks and operations, futures need something called an [ExecutionContext](http://doc.akka.io/docs/akka/2.4.10/scala/futures.html). You can import the existing ``client.executionContext`` as implicit in scope or create your own.


## Establish connections

```scala
import scala.concurrent.Future
import client.executionContext

val conn1: Future[Connection] = client
  .connect(
    url = "ws://localhost:8080/router",
    subprotocol = "wamp.2.json")
    
val conn2 = client
  .connect(
    url = "wss://secure.host.net:443/wamp",
    subprotocol = "wamp.2.msgpack")    
```

The client provides the ``connect()`` method which accepts ``url`` and ``subprotocol`` arguments as documented for the [``Connect``](../../messages#Connect) message constructor. You can create as many connections as you wish (to the same or to different routers) as the connect method returns a distinct (future of) connection.

### Recover connection
You can either recover or _"give up"_ when the (future of) connection fails. To recover from failures (such as ``ConnectionException`` thrown when router doesn't accept) you can compose ``recoverWith`` to attempt another connection (maybe to a fallback router):

```scala
val conn: Future[Connection] = client
  .connect("ws://localhost:8080/router")
  .recoverWith { 
    case ex: ConnectionException => client
      .connect(
        url = "ws://fallback.host.net:9999/ws")
  }
```

As last resort, instead of recovering, you could decide to _"give up"_ a callback function ``onFailure`` that terminates the application:

```scala
conn.onFailure {
  case ex: Throwable =>
    client.terminate().map(_ => System.exit(-1))
}
```

## Open sessions

```scala
val session1: Future[Session] = conn1.flatMap(
  _.openSession(
    realm = "akka.wamp.realm",
    roles = Set("subscriber")))
    
val session2 = conn2.flatMap(
  _.openSession(
    roles = Set("publisher", "callee")))    
```

A (future of) connection can be mapped to a (future of) session by just invoking the ``openSession()`` method which accepts ``realm`` and ``roles`` arguments as documented for the [``Hello``](../../messages#Hello) message constructor. 

> __Note__  
> You can open _only one session per connection_, therefore if you wish to open a second session then you shall use a second connection.


### Shortcut connect and open
You can shortcut connection establishment and session opening in one single concise statement (invoking ``openSession`` on the client rather than on the connection) as follows:

```scala
val session1: Future[Session] = client
  .openSession(
    url = "ws://some.host.net:8080/ws",
    subprotocol = "wamp.2.json",
    realm = "akka.wamp.realm",
    roles = Set("subscriber", "caller"))
```

The ``openSession()`` method accepts all of the ``url``, ``subprotocol``, ``realm`` and ``details`` arguments as mentioned above. It establishes a new connection and opens a new session each time you call it.


### Recover session
You can either recover or _"give up"_ when the (future of) session fails. To recover from failures (such as ``AbortException`` thrown when router doesn't attach to a realm) you can compose ``recoverWith``  to attempt another session opening (maybe to a fallback realm):

```scala
val session: Future[Session] = client
  .openSession(
    realm = "akka.wamp.realm")
  .recoverWith { 
    case ex: AbortException => client
      .openSession(
        realm = "fallback.realm")
  }
```

As last resort, instead of recovering, you could decide to _"give up"_ a callback function ``onFailure`` that just prints a log message:

```scala
session.onFailure {
  case ex: Throwable => 
    client.log.error(ex.getMessage, ex)
}
```


## Subscribe topics
```scala
import akka.wamp._
import akka.wamp.message._

val subscription: Future[Subscription] = session.flatMap(
  _.subscribe(
    topic = "myapp.topic",
    options = Dict()) { 
    event =>
      client.log.info(s"${event.publicationId}")
      event.payload.map { p =>
        p.arguments.map { args =>
          client.log.info(args.mkString)
        }
        p.argumentsKw.map { argsKw =>
          client.log.info(argsKw.mkString)
        }
      }  
    })
```

A (future of) session can be mapped to a (future of) subscription by just invoking the ``subscribe`` method. It is a curried method with two parameter lists.

```scala
def subscribe(topic: String, options: Dict)(handler: EventHandler)
```

The first parameter list accepts ``topic`` and ``options`` arguments as documented for the [``Subscribe``](../../messages#Subscribe) message constructor. The second parameter list accept a callback handler function of type ``EventHandler`` which gets invoked to process each event from the topic. The ``event`` object provides a (option of) ``payload`` which in turn provides both (future of) ``arguments`` and ``argumentsKw``. 

### Recover subscription
You can either recover or _"give up"_ when the (future of) subscription fails. To recover from failures (such as ``SessionException`` thrown when session turn out to be closed) you can compose ``recoverWith`` to attempt another session opening (maybe to a fallback realm and/or to a fallback topic):

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
    client.log.error(ex.getMessage, ex)
}
```

## Unsubscribe topics
TODO

## Publish events
```scala
import akka.Done
import akka.wamp.serialization._

val publication: Future[Either[Done, Publication]] = session.flatMap(
  _.publish(
    topic = "myapp.topic",
    payload = Some(Payload(List("paolo", 40, true))),
    ack = true
  ))
```

A (future of) session can be mapped to a (future of) either done or publication by just invoking the ``publish`` method which accepts ``topic``, ``ack`` and (option of) ``payload`` arguments as documented for the [``Publish``](../../messages#Publish) message constructor.


### Send payload arguments
TDB


### Acknowledge publications

Note that if you leave ``ack`` switched off (as by default) then Akka Wamp will not expect to receive the [``Published``](../../messages#Publish) message back from the router and the (future of either of) publication or done immediately completes with (left of) ``Done``. Otherwise, if you switch ``ack`` on then the (future of either of) publication or done later completes with (rith of) ``Publication`` (if no exception were thrown).

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


### Recover publication

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
    client.log.error(ex.getMessage, ex)
}
```

## Register procedures
TBD


## Call procedures
TBD


## Close sessions
```scala
val session2 = for {
  conn1 <- session1.close()
  session2 <- conn1.openSession()
} yield session2
```

You can close a session but keep the connection to open a session again.


## Disconnect transports
```scala
val conn2 = for {
  client <- conn1.disconnect()
  conn2 <- client.connect()
} yield conn2
```

You can disconnect a connection but keep the client to connect again.

## Terminate the client
```scala
client.terminate().map(_ => System.exit(-1))
```

