TBD 

## For the _impatients_
Let's connect a transport, open a session, subscribe a topic and receive events:

```scala
import akka.actor._
import akka.io._
import akka.wamp._

implicit val system = ActorSystem("myapp")

val client = system.actorOf(Props[MyClient])
IO(Wamp) ! Wamp.Connect(client, "ws://host:9999/router")

class MyClient extends Actor {
  var router: ActorRef = _
  def receive = {
    case Wamp.Connected(r) =>
      router = r
      IO(Wamp) ! Wamp.Disconnect
  }
}
```