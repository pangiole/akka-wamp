[![Build Status][travis-image]][travis-url] [![Codacy Status][codacy-image]][codacy-url]

Akka Wamp is a WAMP - [Web Application Messaging Protocol](http://wamp-proto.org/) implementation written in [Scala](http://scala-lang.org/) with [Akka](http://akka.io/)

## Client
Akka Wamp provides an API specifically designed for [Akka Actor](http://doc.akka.io/docs/akka/2.4.8/scala/actors.html) users. 

Detailed documentation is published [here](http://angiolep.github.io/projects/akka-wamp/index.html)

Following is just a an _"hello world"_ example:

```scala
import akka.actor._
import akka.io._
import akka.wamp._

object HelloApp extends App {
  import Wamp._
  import messages._
  
  implicit val system = ActorSystem("hello")
  val client = system.actorOf(Props(classOf[Client]))
  IO(Wamp) ! Connect(client, url = "ws://127.0.0.1:8080/ws")

  class Client extends Actor with ActorLogging with SessionScope {
    import context.dispatcher
    import scala.concurrent.duration._

    // it sends a "tick" to itself at each second ... forever
    context.system.scheduler.schedule(500 millis, 1000 millis, self, "tick")

    var counter = 0
    var transport: ActorRef = _

    def receive: Receive = {
      case Connected(transport) =>
        this.transport = transport
        transport ! Hello("realm1")

      case welcome: Welcome =>
        log info welcome.toString
        context become {
          case Subscribed(_, _) =>
            log info "subscribed to topic 'onhello'"

          case "tick" =>
            counter = counter + 1
            val payload = Some(Payload(counter))
            transport ! Publish(nextId, topic = "com.example.oncounter", payload)
            log info s"published to 'oncounter' with counter $counter"
        }
        transport ! Subscribe(nextId, topic = "com.example.onhello")
    }
  }
}
```


## Router
Akka Wamp also provides a router that can be either embedded into your application or launched as standalone server process. 

Detailed documentation is published [here](http://angiolep.github.io/projects/akka-wamp/index.html).


## Limitations

 * It works with Scala 2.11 only.
 * It provides WebSocket transport only without SSL/TLS encryption.  
 * The router works as _broker_ only (_dealer_ is NOT provided yet).
 * The client works as _publisher_ and _subscriber_ only (_callee_ and _caller_ are NOT provided yet).
 * It implements the WAMP Basic Profile only (WAMP Advanced Profile is NOT provided yet)
 


[travis-image]: https://travis-ci.org/angiolep/akka-wamp.svg?branch=master
[travis-url]: https://travis-ci.org/angiolep/akka-wamp

[codacy-image]: https://api.codacy.com/project/badge/grade/f66d939188b944bbbfacde051a015ca1
[codacy-url]: https://www.codacy.com/app/paolo-angioletti/akka-wamp
