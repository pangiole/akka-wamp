package examples


/*
 * 
 * sbt -Dakka.loglevel=DEBUG
 * > examples/runMain examples.ActorBasedClientApp
 * 
 */
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