Hello everybody,

I'm glad to let you know I just released the next version of my ``akka-wamp`` implementation for Scala developers.

https://github.com/angiolep/akka-wamp

Major change is the provisioning of a _WAMP Client API_ which I wrote with the purpose to attract as many Scala developers as possible. I'm pretty sure that most of them will like to use Akka abstractions (actors, futures and streams) to write WAMP clients. That's why I started to provide an API based on Akka Actors. Developers will be able to send/receive WAMP message and hold conversational state with very little and elegant Scala code which could look like the following:
 
```
import akka.wamp
import akka.wamp._

class MyClient extends wamp.ClientActor("ws://some.router:8080") {
  
  var reqId = 1
  var evtCount = 0
  
  def connected: Receive = {  
    case Welcome(sid, details) =>
      println(s"Session $sid open ... subscribing to topic")
      transport ! Subscribe(reqId, "my.topic")
    
    case Event(_, _, _, Some(Payload(args)) =>
      evtCount = evtCount + 1
      println(s"$evtCount : $args")
  }
}
```

I also wish to provide an additional _Future-based Client API_ for those developers who don't need to hold conversational state and prefer a simpler asynchronous callbacks solution (something like _promises_ for Javascript developers), but that won't be ready before September.


Cheers
Paolo