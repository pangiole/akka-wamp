# Session Handling
Easily access the manager, establish connections and open sessions.

```scala
import akka.actor._
import akka.io._
import akka.wamp._
import akka.wamp.client._
import akka.wamp.messages._

implicit val system = ActorSystem()
system.actorOf(Props[Client])

class Client extends Actor with ClientUtils {
  val manager = IO(Wamp)
  manager ! Wamp.Connect("ws://localhost:8080/router", "wamp.2.json")
  
  override def receive = {
    case Wamp.Connected(transport) =>
      router = transport
      router ! Hello("myapp.real")
       
    case Welcome(sid, details) =>
      println(s"session $sid established")
  }
}
```

## Access the manager
Akka Wamp provides you with a [Akka I/O](http://doc.akka.io/docs/akka/current/scala/io.html) extension driver named ``Wamp``.

Every Akka I/O driver (TCP, UDP, WAMP, etc.) has a special actor, called a _manager_, that serves as an entry point for the API. The manager for a particular driver is accessible through the ``IO`` entry point. For example the following code looks up the WAMP manager and returns its ActorRef:

```scala
implicit val system = ActorSystem()
val manager = IO(Wamp)
```

## Establish connections


The manager receives I/O command messages and instantiates worker actors in response. The worker actors present themselves to the API user in the reply to the command that was sent. 

```scala
manager ! Wamp.Connect("ws://localhost:8080/router", "wamp.2.json")
```

For example after a ``Connect`` command sent to the manager, the manager creates a worker actor representing the transport handler. All operations related to the given TCP connections can be invoked by sending messages to the transport handler which announces itself via a ``Connected`` signal.

```scala
override def receive = {
  case Wamp.Connected(transport) =>
    router = transport
  }
  // ...
}
```

## Open sessions
TBD

## Close sessions
TBD


## Disconnect transports
TBD   
