# Actor based API overview

Akka Wamp Actor based API provides you with a [Akka I/O](http://doc.akka.io/docs/akka/current/scala/io.html) extension driver named ``Wamp``.


The Akka I/O API is completely [Akka Actor](http://doc.akka.io/docs/akka/current/scala/actors.html) based, meaning that all operations are implemented with message passing instead of direct method calls. 

So it means, you client application spawns a client actor able to 

* __send__ messages such as [Connect](../../messages#Connect), [Hello](../../messages#Hello), [Subscribe](../../messages#Subscribe), [Register](../../messages#Register), [Call](../../messages#Call), [Goodbye](../../messages#Goodbye), [Disconnect](../../messages#Disconnect) to perform all of the WAMP relevant actions such as connecting a transport, opening a session, subscribing to a topic, registering or calling a procedure, closing a session and disconnecting a transport,

* and __receive__ messages such as [Connected](../../messages#Connected), [Abort](../../messages#Abort), [Welcome](../../messages#Welcome), [Subscribed](../../messages#Subscribed), [Registered](../../messages#Registered), [Result](../../messages#Result), [Goodbye](../../messages#Goodbye), [Disconnected](../../messages#Disconnected) to react to all of the WAMP signals such as router connected, session aborted or opened, topic subscribed, procedure registered, result, session closed and transport disconnected.  


```scala
object ActorBasedClientApp extends App {
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
    
    var router: ActorRef = _
    var requestId: Id = _
    var subscriptionId: Id = _
    
    override def receive = {
      case Wamp.Connected(transport) =>
        router = transport
        router ! Hello("myapp.realm")

      case Welcome(_, _) =>
        requestId = nextRequestId()
        router ! Subscribe(requestId, Dict(), "myapp.topic1")
        router ! Publish(nextRequestId(), Dict(), "myapp.topic2")

      case Subscribed(reqId, subId)  =>
        if (reqId == requestId) 
          subscriptionId = subId

      case Event(subId, _, _, payload) =>
        if (subId == subscriptionId)
          payload.parsed.map(p => println(p.args))
    }
  }
}
```

## Read on

* [Session Handling](../actor/session)
* [Publish Subscribe](../actor/pubsub)
* [Remote Procedure Call](../actor/rpc)
* [Payload Handling](../actor/payload)