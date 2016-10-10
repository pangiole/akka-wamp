# Session Handling
Access the manager, connect transports and open sessions.

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
  manager ! Connect("ws://localhost:8080/router", "wamp.2.json")
  
  var transport: ActorRef = _
  val sessionId: Id = _
  
  override def receive = {
    case Connected(handler) =>
      transport = handler
      println("transport connected")
      transport ! Hello("myapp.realm")
       
    case Welcome(sid, details) =>
      sessionId = sid  
      println(s"session $sid opened")
  }
}
```

## Access the manager
Akka Wamp provides you with a [Akka I/O](http://doc.akka.io/docs/akka/current/scala/io.html) extension driver named ``Wamp``.

Every Akka I/O driver (TCP, UDP, WAMP, etc.) has a special actor, called __extension manager__, that serves as an entry point for the API. The manager for a particular driver is accessible through the ``IO`` entry point. For example the following code looks up the WAMP manager and returns its ActorRef:

```scala
implicit val system = ActorSystem()
val manager = IO(Wamp)
```

## Connect transports

The manager receives I/O command messages and instantiates worker actors in response. The worker actors present themselves to the API user in the reply to the command that was sent. 

```scala
manager ! Connect("ws://localhost:8080/router", "wamp.2.json")
```

For example, after a ``Connect`` command is sent to the manager, the manager creates a worker actor representing the transport handler. All operations related to the WAMP protocol can be invoked by sending messages to the transport handler. The handler which announces itself via a ``Connected`` signal.

```scala
var transport: ActorRef = _

override def receive = {
  case Connected(handler) =>
    transport = handler
    println("transport connected")
    // ...
}
```

## Open sessions
```scala
transport ! Hello("myapp.realm")
```

TBD

```scala
val sessionId: Id = 0L

override def receive = {
  // ...
  case Welcome(sid, details) =>
    sessionId = sid  
    println(s"session $sid opened")
    // ...
}
```

## Close sessions
```scala
transport ! Goodbye()
```

TBD

```scala
override def receive = {
  // ...
  case Goodbye(details, reason) =>
    sessionId = 0L
    // ...
}
```

## Disconnect transports
```scala
transport ! Disconnect
```

TBD

```scala
override def receive = {
  // ...
  case Disconnected =>
    // ...
}
```