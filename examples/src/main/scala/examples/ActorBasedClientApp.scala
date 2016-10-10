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
    manager ! Connect("ws://localhost:8080/router", "wamp.2.json")
    
    var transport: ActorRef = _
    var requestId: Id = _
    var subscriptionId: Id = _
    
    override def receive = {
      case signal @ Connected(handler) =>
        transport = handler
        transport ! Hello("myapp.realm")

      case Welcome(_, _) =>
        requestId = nextRequestId()
        transport ! Subscribe(requestId, Dict(), "myapp.topic1")
        transport ! Publish(nextRequestId(), Dict(), "myapp.topic2")

      case Subscribed(reqId, subId)  =>
        if (reqId == requestId) 
          subscriptionId = subId

      case Event(subId, _, _, payload) =>
        if (subId == subscriptionId)
          payload.parsed.map { p => 
            if (p.args(0).toString == "Everybody out!") 
              transport ! Disconnect
          }

      case signal @ Disconnected =>
        self ! PoisonPill
    }

    @scala.throws[Exception](classOf[Exception])
    override def postStop(): Unit = {
      system.terminate().map(t => System.exit(0))
    }
  }
}