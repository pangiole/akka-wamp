/*
 * 
 * sbt -Dakka.loglevel=DEBUG
 * > examples_actor/runMain examples.ActorBasedClientApp
 * 
 */
object ActorBasedClientApp extends App {
  import akka.actor._
  import akka.io._
  import akka.wamp._
  import akka.wamp.client._
  import akka.wamp.messages._
  import scala.concurrent.duration._

  implicit val system = ActorSystem("examples")
  system.actorOf(Props[Client], name = "client")

  
  class Client extends Actor with ActorLogging with ClientContext {
    import Client._
    
    val manager = IO(Wamp)

    override def preStart(): Unit = {
      self ! DoConnect      
    }
    
    override def receive(): Receive = {
      case DoConnect =>
        manager ! Connect("ws://localhost:8080/router", "wamp.2.json")

      case signal @ CommandFailed(cmd: Connect, ex) =>
        log.warning(ex.getMessage)
        scheduler.scheduleOnce(1 second, self, DoConnect)
        
      case signal @ Connected(transport) =>
        log.info(s"Connected $transport")
        context become handleConnected(transport)
        transport ! Hello("default.realm")
    }

    
    var requestId: Id = _
    def handleConnected(transport: ActorRef): Receive = {
      case Welcome(sessionId, details) =>
        context become handleSession(transport, sessionId)
        requestId = nextRequestId()
        transport ! Subscribe(requestId, Dict(), "myapp.topic1")
        // scheduler.schedule(1 second, 1 second, transport, Publish(nextRequestId(), Dict(), "myapp.topic2"))

      case msg @ Abort(details, reason) =>
        log.warning(msg.toString)
        self ! PoisonPill
        
      case signal @ Disconnected =>
        log.info(s"Disconnected")
        self ! PoisonPill
    }


    var subscriptionId: Id = _
    def handleSession(transport: ActorRef, sessionId: Id): Receive = {
      case Subscribed(reqId, subId)  =>
        if (reqId == requestId)
          subscriptionId = subId

      case Event(subId, _, _, payload) =>
        if (subId == subscriptionId)
          payload.parsed.map { p =>
            if (p.args(0).toString == "Everybody out!") {
              context become handleDisconnecting
              transport ! Disconnect
            }
          }

      case signal @ Disconnected =>
        log.info(s"Disconnected")
        self ! PoisonPill  
    }
    
    
    def handleDisconnecting: Receive = {
      case signal @ Disconnected =>
        log.info(s"Disconnected")
        self ! PoisonPill
    }
    
    @scala.throws[Exception](classOf[Exception])
    override def postStop(): Unit = {
      system.terminate().map(t => System.exit(0))
    }
  }
  
  object Client {
    case object DoConnect
  }
}
