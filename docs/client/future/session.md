# Session Handling
Create clients, connect transport and open sessions.

```scala
import akka.wamp.client._
val client = Client("myapp")

implicit val ec = client.executionContext

val session = client
  .connect(
    url = "ws://localhost:8080/router",
    subprotocol = "wamp.2.json")
  .openSession(
    realm = "default.realm",
    roles = Set("subscriber")
  )
}
```


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
In Akka Wamp, one client can connect many transports but each transport can open only one session.

```
    ,--------.  1      0..n  ,------------.  1      0..1  ,---------. 
    | Client |  -----------  | Transport  |  -----------  | Session | 
    `--------'               `------------'               `---------'
```

Bear in mind that, though you could create as many client as you wish, its actor system is a heavyweight structure that allocates 1..n threads. So you're advised to create _one client per logical application_.

 
 
### Execution context
All of the operation provided by client, transport and session objects always return futures. In order to execute callbacks and operations, futures need something called an [ExecutionContext](http://doc.akka.io/docs/akka/2.4.10/scala/futures.html). You can import the existing ``client.executionContext`` as implicit in scope:

```scala
implicit val ec = client.executionContext
```

or create your own.


## Connect transports

```scala
import scala.concurrent.Future

val transport1: Future[Transport] = client
  .connect(
    url = "ws://localhost:8080/router",
    subprotocol = "wamp.2.json")
    
val transport2 = client
  .connect(
    url = "wss://secure.host.net:443/wamp",
    subprotocol = "wamp.2.msgpack")    
```

Connect a transport by just invoking the client ``connect()`` method which accepts ``url`` and ``subprotocol`` arguments as documented for the [``Connect``](../../messages#Connect) message constructor. 

You can establish as many connections as you wish (to the same or to different routers) as the connect method returns a distinct (future of) connection.

### Recover
You can either recover or _"give up"_ when the (future of) connection fails. To recover from failures (such as ``TransportException`` thrown when router cannot accept connection) you can compose ``recoverWith`` to attempt another connection (maybe to a fallback router):

```scala
val transport1: Future[Transport] = client
  .connect(
    url = "ws://localhost:8080/router")
  .recoverWith { 
    case ex: TransportException => client
      .connect(
        url = "ws://fallback.host.net:9999/path")
  }
```

As last resort, instead of recovering, you could decide to _"give up"_ a callback function ``onFailure`` that terminates the application:

```scala
transport.onFailure {
  case ex: Throwable =>
    client.terminate().map(_ => System.exit(-1))
}
```

## Open sessions

```scala
val session1: Future[Session] = conn1.flatMap(
  _.openSession(
    realm = "default.realm",
    roles = Set("subscriber")))
    
val session2 = conn2.flatMap(
  _.openSession(
    roles = Set("publisher", "callee")))    
```

A (future of) transport can be mapped to a (future of) session by just invoking the ``openSession()`` method which accepts ``realm`` and ``roles`` arguments as documented for the [``Hello``](../../messages#Hello) message constructor. 

You can open only one session per transport. Therefore, if you wish to open a second session then you must establish a second transport (using the same client or a different one).


### Shortcut
You can shortcut transport connection and session opening in one single concise statement by invoking ``openSession()`` on the client rather than on the transport:

```scala
val session: Future[Session] = client
  .openSession(
    url = "ws://some.host.net:8080/ws",
    subprotocol = "wamp.2.json",
    realm = "default.realm",
    roles = Set("subscriber", "caller"))
```

The client ``openSession()`` method accepts all of the ``url``, ``subprotocol``, ``realm`` and ``details`` arguments mentioned above. It connects a new transport and opens a new session each time you call it.


### Recover
You can either recover or _"give up"_ when the (future of) session fails. To recover from failures (such as ``AbortException`` thrown when router doesn't attach to a realm) you can compose ``recoverWith`` to attempt another session opening (maybe to a fallback realm):

```scala
val session: Future[Session] = client
  .openSession(
    realm = "default.realm")
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
```scala
val transport: Future[Transport] = session.close()
```

## Disconnect transports
```scala
val d: Future[Disconnected] = transport.disconnect()
```

## Terminate clients
```scala
val t: Future[Terminated] = client.terminate()
```
