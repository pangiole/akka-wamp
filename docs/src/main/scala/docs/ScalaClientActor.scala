package docs

// #client
import java.net.URI

import akka.actor._
import akka.io._
import akka.wamp._
import akka.wamp.client._
import akka.wamp.messages._

class ScalaClientActor extends ClientActor with ActorLogging {
  private var conn: ActorRef = _
  private var sessionId: Long = _
  
  // def receive ...

  // #client
  // #connect
  override def preStart(): Unit = {
    implicit val system = context.system
    val manager = IO(Wamp)
    manager ! Connect(new URI("ws://router.net:8080/wamp"), "json")
  }
  
  // #open
  override def receive: Receive = {
    // #open
    case CommandFailed(cmd, ex) =>
      // reattempt connection ...
      
      // #open
    case Connected(conn, _, _) =>
      this.conn = conn
      context become connected
      // #connect
      conn ! Hello("default")
    // #connect 
  }
  // #connect

  // #subscribe
  private var requestId: Id = _
  private var subscriptionId: Id = _
  
  // #publish
  private def connected: Receive = {
    // #publish
    // #subscribe
    case Disconnected =>
      this.sessionId = 0
      this.conn = null
      // reattempt connection ...
      // reopen session ...
      // restore subscriptions/registrations ...
      
    case Abort(details, reason) =>
      this.sessionId = 0
      log.warning(reason)
      self ! PoisonPill

    // #subscribe  
    // #publish
    case Welcome(sessionId, details) =>
      this.sessionId = sessionId
      context become open
      // submit subscriptions/registrations ...
      // #publish
      // #open
      this.requestId = nextRequestId()
      conn ! Subscribe(this.requestId, Subscribe.defaultOptions, "myapp.topic")
      // #subscribe
      // #publish
      conn ! Publish(nextRequestId(), Publish.defaultOptions, "myapp.topic")
      conn ! Publish(nextRequestId(), Publish.defaultOptions.withAcknowledge(true), "myapp.topic")
      // #publish
      conn ! Register(nextRequestId(), Register.defaultOptions, "myapp.procedure")
      // #subscribe
      // #publish
      // #open
  }
  // #open
  // #publish
  // #subscribe
  
  // #subscribe
  
  private def open: Receive = {
    case Disconnected =>
      // ...
      
    case Goodbye(details, reason) =>
      this.sessionId = 0
      log.warning(reason)
      // reopen session ...
      // restore subscriptions/registrations ...

    case Error(Subscribe.tpe, requestId, details, error, _) =>
      if (this.requestId == requestId) {
        log.warning(error)
        self ! PoisonPill
      }
      
    case Subscribed(requestId, subscriptionId) =>
      if (this.requestId == requestId) {
        this.subscriptionId += subscriptionId
        context become subscribed
        // OR become anyElseYouLike
      }
  }
  // #subscribe
  
  private def subscribed: Receive = {
    case "" => ???
  }
  
  // #client
}
// #client