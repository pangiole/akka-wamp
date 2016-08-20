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
class Transport private[client] (client: ActorRef, router: ActorRef) extends akka.wamp.TransportLike {
  private val log = LoggerFactory.getLogger(classOf[Transport])

  private val DefaultRoles = Set(Publisher, Subscriber)

  private[client] val clientRef: ActorRef = client
  
  private[client] val routerRef: ActorRef = router
  
  /**
    * Open a new session sending an HELLO message to the remote router 
    * for the given realm and roles, then return a future of session.
    * 
    * @param realm is the realm to attach the session to
    * @param roles is the roles set
    * @return a future of session               
    */
  def open(realm: Uri = "akka.wamp.realm", roles: Set[Role] = DefaultRoles): Future[Session] = {
    val promise = Promise[Session]
    try {
      val hello = Hello(realm, Dict().withRoles(roles.toList: _*))
      become {
        welcomeHandler(promise) orElse
          abortReceive(promise) orElse
            unexpectedReceive(promiseToBreak = Some(promise))
      }
      log.debug("--> {}", hello)
      routerRef ! hello
    } catch {
      case ex: Throwable =>
        log.debug(ex.getMessage)
        promise.failure(new TransportException(ex.getMessage))
    }
    promise.future
  }


  
  private def welcomeHandler(promise: Promise[Session]): Receive = {
    case welcome: Welcome =>
      log.debug("<-- {}", welcome)
      val session = new Session(this, welcome)
      become {
        goodbyeReceive(session) orElse
          unexpectedReceive(None)
      }
      promise.success(session)
  }
  
  
  private[client] var receive: Receive =  _

  private[client] def become(receive: Receive) = {
    this.receive = receive
  }
  
  private[client] def !(msg: Message) = {
    log.debug("--> {}", msg)
    routerRef ! msg
  }
  
  
  private[client] def abortReceive(promise: Promise[Session]): Receive = {
    case abort: Abort =>
      log.debug("<-- {}", abort)
      promise.failure(new AbortException(abort))
  }

  
  private[client] def goodbyeReceive(session: Session): Receive = {
    case msg: Goodbye =>
      log.debug("<-- {}", msg)
      /*
       * A session ends when the underlying transport disappears or 
       * when it is closed explicitly by a "GOODBYE" message sent by 
       * one _Peer_ and a "GOODBYE" message sent from the other _Peer_ 
       * in response.
       */
      routerRef ! Goodbye("wamp.error.goodbye_and_out")
      session.doClose()
  }
  
  private[client] def unexpectedReceive[T](promiseToBreak: Option[Promise[T]]): Receive = {
    case msg =>
      log.warn("!!! {}", msg)
      promiseToBreak.map(_.failure(new Exception(s"Unexpected message $msg")))
  }
}
