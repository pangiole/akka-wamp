package examples

/**
  * Created by paolo on 21/12/2016.
  */
object ScalaActorsApp extends App {
  import akka.actor._
  import akka.io._
  import akka.wamp._
  import akka.wamp.client._
  import akka.wamp.messages._

  import scala.concurrent.duration._

  val system = ActorSystem("examples")
  system.actorOf(Props[Client], name = "client")


  class Client extends Actor with ActorLogging with ClientActor {
    import Client._

    var attempts = 0

    override def preStart(): Unit = {
      self ! AttemptConnection
    }

    override def receive(): Receive = {
      case AttemptConnection =>
        if (attempts < MaxAttempts) {
          attempts = attempts + 1
          log.info("Connection attempt #{} to {}", attempts, RouterUrl)
          IO(Wamp) ! Connect(RouterUrl, "json")
        }
        else {
          log.warning("Max connection attempts reached!")
          self ! PoisonPill
        }

      case sig @ CommandFailed(cmd: Connect, ex) =>
        log.warning(ex.getMessage)
        scheduler.scheduleOnce(1 second, self, AttemptConnection)

      case sig @ Connected(conn) =>
        log.info("Connected {}", conn)
        attempts = 0
        context become handleConnection(conn)
        conn ! Hello("default")
    }


    var requestId: Id = _
    def handleConnection(conn: ActorRef): Receive = {
      case sig @ Disconnected =>
        log.warning("Disconnected")
        self ! AttemptConnection

      case msg @ Abort(details, reason) =>
        log.warning(msg.toString)
        self ! PoisonPill

      case msg @ Welcome(sessionId, details) =>
        log.info("Session #{} open", sessionId)
        context become handleSession(conn, sessionId)
        requestId = nextRequestId()
        conn ! Subscribe(requestId, Dict(), "myapp.topic1")
        // scheduler.schedule(1 second, 1 second, transport, Publish(nextRequestId(), Dict(), "myapp.topic2"))
    }


    var subscriptionId: Id = _
    def handleSession(conn: ActorRef, sessionId: Id): Receive = {
      case Subscribed(reqId, subId)  =>
        if (reqId == requestId)
          subscriptionId = subId

      case evt @ Event(subId, _, _, _) =>
        if (subId == subscriptionId)
          evt.args.map { args =>
            if (args(0).toString == "Everybody out!") {
              context become handleDisconnecting
              conn ! Disconnect
            }
          }

      case sig @ Disconnected =>
        log.info("Disconnected")
        self ! PoisonPill
    }


    def handleDisconnecting: Receive = {
      case sig @ Disconnected =>
        log.info(s"Disconnected")
        self ! PoisonPill
    }

    override def postStop(): Unit = {
      system.terminate().map(t => System.exit(0))
    }
  }

  object Client {
    val RouterUrl = "ws://localhost:8080/wamp"
    val MaxAttempts = 8
    case object AttemptConnection
  }
}
