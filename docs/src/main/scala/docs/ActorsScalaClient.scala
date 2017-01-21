package docs

// #client
import java.net.URI

import akka.actor._
import akka.io._
import akka.wamp._
import akka.wamp.client._
import akka.wamp.messages._

class ActorsScalaClient extends ClientActor with ActorLogging {

  // def receive: Receive = ...
  // #client

  // #connect
  private var router: ActorRef = _


  // #connect

  // #open

  private var gen: SessionScopedIdGenerator = _
  private var sessionId: Long = _

  // #open


  // #subscribe
  private var requestId: Id = _
  private var subscriptionId: Id = _

  // #subscribe

  // #connect
  override def preStart(): Unit = {
    implicit val system = context.system
    val manager = IO(Wamp)
    manager ! Connect(new URI("ws://router.net:8080/wamp"), "json")
  }

  // #open
  override def receive: Receive = {
    // #open
    case sig @ CommandFailed(cmd, ex) =>
      // could reattempt connection ...
      
      // #open
    case sig @ Connected(handler, uri, format) =>
      this.router = handler
      context become connected
      // #connect
      router ! Hello("default")
    // #connect 
  }
  // #connect

  // #subscribe
  // #publish
  private def connected: Receive = {
    // #publish
    // #subscribe
    case Disconnected =>
      this.sessionId = 0
      this.router = null
      // reattempt connection ...
      // reopen session ...
      // restore subscriptions/registrations ...
      
    case Abort(details, reason) =>
      this.sessionId = 0
      // open new sesson ...

    // #subscribe
    // #publish
    case Welcome(sessionId, details) =>
      this.sessionId = sessionId
      this.gen = new SessionScopedIdGenerator
      context become open
      // submit subscriptions/registrations ...
      // #publish
      // #open
      this.requestId = gen.nextId()
      router ! Subscribe(this.requestId, Subscribe.defaultOptions, "myapp.topic")
      // #subscribe
      // #publish
      router ! Publish(gen.nextId(), Publish.defaultOptions, "myapp.topic")
      router ! Publish(gen.nextId(), Publish.defaultOptions.withAcknowledge(true), "myapp.topic")
      // #publish
      router ! Register(gen.nextId(), Register.defaultOptions, "myapp.procedure")
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
      // open new sesson ...
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