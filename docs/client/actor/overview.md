# Overview

Akka Wamp Actor based API provides you with a [Akka I/O](http://doc.akka.io/docs/akka/current/scala/io.html) extension driver named ``Wamp``.


The Akka I/O API is completely [Akka Actor](http://doc.akka.io/docs/akka/current/scala/actors.html) based, meaning that all operations are implemented with message passing instead of direct method calls. Every Akka I/O driver (TCP, UDP, WAMP, et.c) has a special actor, called a _manager_, that serves as an entry point for the API. The manager for a particular driver is accessible through the ``IO`` entry point. For example the following code looks up the WAMP manager and returns its ActorRef:

```scala
implicit val system = ActorSystem()
val manager = IO(Wamp)
```

The manager receives I/O command messages and instantiates worker actors in response. The worker actors present themselves to the API user in the reply to the command that was sent. 

```scala
manager ! Wamp.Connect("ws://localhost:8080/router", "wamp.2.json")
```

For example after a ``Connect`` command sent to the WAMP manager the manager creates an actor representing the WAMP connection. All operations related to the given TCP connections can be invoked by sending messages to the connection actor which announces itself by sending a ``Connected`` signal message.

## ClientApp

```scala
object ClientApp extends App {
  import akka.actor._
  import akka.io._
  import akka.wamp._

  implicit val system = ActorSystem()
  system.actorOf(Props[ClientActor])
  
  class ClientActor extends Actor {
    val manager = IO(Wamp)
    manager ! Wamp.Connect("ws://localhost:8080/router", "wamp.2.json")
    
    var conn: ActorRef = _
        
    override def receive = {
      case signal @ Wamp.Connected(c) =>
        conn = c
    }
  }
}
```

Once your client is connected to the router, you can open a session, publish/subscribe to topics, register/call procedures, handle payloads, etc.

## Read on

* [Session Handling](../future/session)
* [Publish Subscribe](../future/pubsub)
* [Remote Procedure Call](../future/rpc)
* [Payload Handling](../future/payload)