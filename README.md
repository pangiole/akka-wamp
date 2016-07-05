# akka-wamp
WAMP - [Web Application Messaging Protocol](http://wamp-proto.org/) implementation written in Scala with Akka HTTP.

[![Build Status][travis-image]][travis-url] [![Codacy Status][codacy-image]][codacy-url]


## Router
It provides a WAMP Router that can be either embedded into your application or launched as standalone server process.

### Embedded
Make your SBT build depend on akka-wamp:
```scala
scalaVersion := "2.11.8"

libraryDependencies += "com.github.angiolep" %% "akka-wamp" % "0.2.1"
```

After having created the Akka actor system, just instantiate and start the router actor as follows:
```scala
import akka.actor._
import akka.wamp.router._

implicit val system = ActorSystem("wamp")
implicit val mat = ActorMaterializer()
system.actorOf(Router.props(), "router")
```
The router actor will automatically bind on a server socket by reading the following Akka configuration

 - akka.wamp.iface
 - akka.wamp.port
 - akka.wamp.subprotocol


### Standalone
Download and launch the router as standalone application:

```bash
curl https://dl.bintray.com/angiolep/universal/akka-wamp-0.2.1.tgz
tar xvfz akka-wamp-0.2.1.tar.gz
cd akka-wamp-0.2.1
./bin/akka-wamp -Dakka.loglevel=DEBUG -Dakka.wamp.port=7070
```

 
## Client APIs
We provide you with three alternative APIs in writing WAMP clients:

 * future based,
 * actor based,
 * and stream based.


### Future based
Working in progress


### Actor based
If you wish your client to be stateful (able to hold conversational state) the we provide you with an actor based API. You can write an actor client, meaning that all operations are implemented with message passing instead of direct method calls. Our API is strongly inspired by Akka IO, as you can see from the following example:

```scala
import akka.actor._
import akka.io._
import akka.wamp._

class Client extends Actor {
  import Wamp._
  import context.system

  IO(Wamp) ! Connect("ws://localhost:7070/wamp", "wamp.2.json")
  
  def receive: Receive = {
   case Connected(transport) =>
     context become connected(transport)
     transport ! Hello("akka.wamp.realm", Dict().withRoles("subscriber"))
     
   case CommandFailed(_: Connect) =>
     context stop self
  }
  
  
  def connected(transport: ActorRef): Receive = {
   case Welcome(sessionId, details) =>
     transport ! Subscribe(1, Dict(), "myapp.topic1")
     
   case Event(_, _, _, Some(Payload(args)) =>
     println(args)
  
   case ConnectionClosed => 
     context.stop(self)
  }
}
```

``Wamp`` is the protocol extension identifier you will pass to the Akka ``IO`` entry point. That will return the ``WampManager`` actor reference to which you'll have to send the ``Connect`` message. The manager will reply with a ``Connected`` message upon connection to pass the transport actor reference. Then, you could make your client actor become connected and finally send the ``Hello`` message to the transport so to open a new WAMP session.

The actor-based approach requires fair knowledge of all the WAMP messages that can be sent to (and received from) the underlying transport. It certainly gives a lot of flexibility when compared to other approaches (such as the future-based approach) as you will not be constraint to a specific API. 

All WAMP messages inherits from the ``Payload`` trait and are provided as case classes you can match via the Scala pattern matching mechanism. Further examples are provided [here](https://github.com/angiolep/akka-wamp/tree/master/examples/main/scala/akka/wamp/client/actor).



### Stream based
Working in progress.


## Limitations

 * It works with Scala 2.11 only.
 * It provides WebSocket transport only without SSL/TLS encryption.  
 * The WebSocketRouter works as _broker_ only (_dealer_ is NOT provided yet).
 * It implements the WAMP Basic Profile only.
 


[travis-image]: https://travis-ci.org/angiolep/akka-wamp.svg?branch=master
[travis-url]: https://travis-ci.org/angiolep/akka-wamp

[codacy-image]: https://api.codacy.com/project/badge/grade/f66d939188b944bbbfacde051a015ca1
[codacy-url]: https://www.codacy.com/app/paolo-angioletti/akka-wamp
