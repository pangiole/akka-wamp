package akka.wamp.client

import akka.actor.Actor.Receive
import akka.actor._
import akka.wamp.Roles._
import akka.wamp._
import akka.wamp.messages._
import org.slf4j.LoggerFactory

import scala.concurrent.{Future, Promise}

/**
  * A Transport connects two [[Peer]]s and provides a channel over which 
  * [[Message]]s for a [[Session]] can flow in both directions.
  */
class Transport private[client] (router: ActorRef) extends akka.wamp.TransportLike {
  private val log = LoggerFactory.getLogger(classOf[Transport])    
  
  /**
    * Send an HELLO message to the remote router for the given realm 
    * and with the given details, then return a future of session.
    * 
    * @param realm
    * @param roles
    * @return a future of session               
    */
  def open(
    realm: Uri = "akka.wamp.realm",
    roles: Set[Role] = Set(Publisher, Subscriber)): Future[Session] = 
  {
    val promise = Promise[Session]
    try {
      val hello = Hello(realm, Dict().withRoles(roles.toList: _*))
      val handleMessages: Receive = {
        case welcome: Welcome =>
          log.debug("<-- {}", welcome)
          promise.success(new Session(this, welcome))
        case abort: Abort =>
          log.debug("<-- {}", abort)
          promise.failure(new AbortException(abort))
        case message =>
          log.debug("<!! {}", message)
          promise.failure(new UnexpectedException(s"Unexpected $message"))
      }
      become(handleGoodbye orElse handleMessages orElse handleUnknown)
      router ! hello
    } catch {
      case ex: Throwable =>
        promise.failure(new OpenException(ex.getMessage))
    }
    promise.future
  }

  private[client] var receive: Receive =  _

  private[client] def become(receive: Receive) = {
    this.receive = receive
  }
  
  private[client] def !(msg: Message) = {
    log.debug("--> {}", msg)
    router ! msg
  }


  private[client] def handleGoodbye: Receive = {
    case msg: Goodbye =>
      log.debug("<-- {}", msg)
      /*
       * A session ends when the underlying transport disappears or 
       * when it is closed explicitly by a "GOODBYE" message sent by 
       * one _Peer_ and a "GOODBYE" message sent from the other _Peer_ 
       * in response.
       */
      router ! Goodbye("wamp.error.goodbye_and_out")
      // TODO shall we close the session?
  }

  private[client] def handleUnknown: Receive = {
    case msg =>
      log.warn("!!! {}", msg)
  }
}
