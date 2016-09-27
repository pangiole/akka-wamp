# Session Handling

```scala
import akka.wamp.client._
val client = Client("myapp")

implicit val ec = client.executionContext

val session = client
  .connect(
    url = "ws://localhost:8080/router",
    subprotocol = "wamp.2.json")
  .openSession(
    realm = "akka.wamp.realm",
    roles = Set("subscriber")
  )
}
```

Create the client and establish connections to open sessions.


## Create clients

```scala
import akka.wamp.client._
val client = Client("myapp")
```

Create the client by invoking the ``Client`` companion object factory method with the following arguments

 * ``name``  
   The unique name of the client (default is ``"default"``)
   
 * ``config``  
   The configuration object (default is ``ConfigFactory.load()`` as per standard behaviour of [TypeSafe Config](https://github.com/typesafehub/config#standard-behavior))

The factory method creates an [Akka ActorSystem](http://doc.akka.io/docs/akka/2.4.10/general/actor-systems.html) named after the client and configured with the given configuration object. 

 
### Configuration
TBD


### Multiplicity
In Akka Wamp, one client can establish many connections but each connection can open only one session.

```
    ,--------.  1      0..n  ,------------.  1      0..1  ,---------. 
    | Client |  -----------  | Connection |  -----------  | Session | 
    `--------'               `------------'               `---------'
```

Bear in mind that, though you could create as many client as you wish, its actor system is a heavyweight structure that allocates 1..n threads. So you're advised to create _one client per logical application_.

 
 
### Execution context
All of the operation provided by client, connection and sessions objects always return futures. In order to execute callbacks and operations, futures need something called an [ExecutionContext](http://doc.akka.io/docs/akka/2.4.10/scala/futures.html). You can import the existing ``client.executionContext`` as implicit in scope:

```scala
implicit val ec = client.executionContext
```

or create your own.


## Establish connections

```scala
import scala.concurrent.Future

val conn1: Future[Connection] = client
  .connect(
    url = "ws://localhost:8080/router",
    subprotocol = "wamp.2.json")
    
val conn2 = client
  .connect(
    url = "wss://secure.host.net:443/wamp",
    subprotocol = "wamp.2.msgpack")    
```

Establish a connection to a router invoking the client ``connect()`` method which accepts ``url`` and ``subprotocol`` arguments as documented for the [``Connect``](../../messages#Connect) message constructor. 

You can establish as many connections as you wish (to the same or to different routers) as the connect method returns a distinct (future of) connection.

### Recover
You can either recover or _"give up"_ when the (future of) connection fails. To recover from failures (such as ``ConnectionException`` thrown when router cannot accept connection) you can compose ``recoverWith`` to attempt another connection (maybe to a fallback router):

```scala
val conn1: Future[Connection] = client
  .connect(
    url = "ws://localhost:8080/router")
  .recoverWith { 
    case ex: ConnectionException => client
      .connect(
        url = "ws://fallback.host.net:9999/path")
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

A (future of) connection can be mapped to a (future of) session by just invoking the connection ``openSession()`` method which accepts ``realm`` and ``roles`` arguments as documented for the [``Hello``](../../messages#Hello) message constructor. 

You can open only one session per connection. Therefore, if you wish to open a second session then you must establish a second connection (using the same client or a different one).


### Shortcut
You can shortcut connection establishment and session opening in one single concise statement by invoking ``openSession()`` on the client rather than on the connection:

```scala
val session: Future[Session] = client
  .openSession(
    url = "ws://some.host.net:8080/ws",
    subprotocol = "wamp.2.json",
    realm = "akka.wamp.realm",
    roles = Set("subscriber", "caller"))
```

The client ``openSession()`` method accepts all of the ``url``, ``subprotocol``, ``realm`` and ``details`` arguments mentioned above. It establishes a new connection and opens a new session each time you call it.


### Recover
You can either recover or _"give up"_ when the (future of) session fails. To recover from failures (such as ``AbortException`` thrown when router doesn't attach to a realm) you can compose ``recoverWith`` to attempt another session opening (maybe to a fallback realm):

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
    log.error(ex.getMessage, ex)
}
```

## Close sessions
TBD

## Disconnect transports
TBD

## Terminate clients
TBD
