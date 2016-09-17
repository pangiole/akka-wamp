Akka Wamp provides you with an [Actor](http://doc.akka.io/docs/akka/2.4.10/scala/actors.html) based API to let you write your client with a low-level API and have full control of it!

## For the _impatients_
Let's connect a transport, open a session, subscribe a topic and receive events:

```scala
import akka.actor._
import akka.io._
import akka.wamp._

implicit val system = ActorSystem("myapp")

val client = system.actorOf(Props[MyClient])
IO(Wamp) ! Wamp.Connect(client, "ws://localhost:8080/router")

class MyClient extends Actor {
  var router: ActorRef = _
  def receive = {
    case signal @ Wamp.Connected(r) =>
      router = r
      IO(Wamp) ! Wamp.Disconnect
  }
}
```