package akka.wamp.client

import akka.actor.Actor.Receive
import akka.actor._
import akka.wamp._
import akka.wamp.messages._
import org.slf4j._

import scala.concurrent._

/**
  * WAMP connections are established by clients to a router.
  * 
  * {{{
  *   import akka.wamp.client._
  *   val client = Client("myapp")
  *
  *   import scala.concurrent.Future
  *   implicit val ec = client.executionContext
  *
  *   val transport: Future[Transport] = client
  *     .connect(
  *       url = "ws://localhost:8080/ws",
  *       subprotocol = "wamp.2.json"
  *     )
  * }}}
  * 
  * WAMP connections can use any transport that is message-based, ordered,
  * reliable and bi-directional, with WebSocket as the default transport.
  * 
  * A WAMP session can be opened during the WAMP connection lifecycle, but
  * only one at the time.
  *
  * @param clientRef is the client actor reference
  * @param transportHandler is transport handler actor reference
  * @param system is the Akka actor system
  * @param validator is the WAMP types validator
  */
class Transport private[client](clientRef: ActorRef, transportHandler: ActorRef)
                               (implicit system: ActorSystem, validator: Validator) 
{
  private val log = LoggerFactory.getLogger(classOf[Transport])

  /** Connection state */
  private[client] var connected = true
  
  
  /**
    * Open a WAMP session with the given realm and roles.
    * 
    * The client sends a HELLO message to the router which
    * in turn replies with a WELCOME or ABORT message.
    *
    * {{{
    *     ,------.              ,------.
    *     |Client|              |Router|
    *     `--+---'              `--+---'
    *        |      HELLO          |
    *        | ------------------> |
    *        |                     |
    *        |   WELCOME / ABORT   |
    *        | <------------------ |
    *     ,--+---.              ,--+---.
    *     |Client|              |Router|
    *     `------'              `------'
    * }}}
    * 
    * @param realm is the realm to attach the session to (default is "default.realm")
    * @param roles is this client roles set (default is all possible client roles)
    * @return the (future of) session or [[AbortException]]
    */
  def openSession(realm: Uri = Hello.defaultRealm, roles: Set[Role] = Roles.client): Future[Session] = {
    withPromise[Session] { promise =>
      try {
        // Hello factory could throw exception because of invalid realm and/or roles
        val hello = Hello(realm, Dict().addRoles(roles.toList: _*))
        become {
          handleWelcome(promise) orElse
            handleAbort(promise) orElse 
              handleDisconnected(promise)
        }
        log.debug("--> {}", hello)
        transportHandler.tell(hello, clientRef)
      } catch {
        case ex: Throwable =>
          log.debug(ex.getMessage)
          promise.failure(new TransportException(ex.getMessage))
      }
    }
  }
  
  /**
    * Disconnect this transport
    * 
    * @return
    */
  def disconnect(): Future[Disconnected] = {
    withPromise[Disconnected] { promise =>
      become /* handleDisconnecting */ 
      {
        case signal @ Disconnected =>
          log.debug("!!! {}", signal)
          connected = false
          clientRef ! PoisonPill
          promise.success(signal)
      }
      transportHandler.tell(Disconnect, clientRef)
    }
  }

  
  /** Create a promise and breaks it if this transport is not connected anymore */
  private def withPromise[T](fn: Promise[T] => Unit) = {
    val promise = Promise[T]
    if (connected) {
      fn(promise)
    } else {
      promise.failure(new TransportException("Disconnected"))
    }
    promise.future
  }
  
  /** Process any message received by the client */
  private[client] var receive: Receive =  _

  
  /** Swap its receive function with the given one */
  private[client] def become(receive: Receive) = {
    this.receive = receive
  }

  /**
    * Send the given message to the router
    */
  private[client] def !(message: Message) = {
    log.debug("--> {}", message)
    transportHandler.tell(message, clientRef)
  }

  
  /** Handle an incoming WELCOME messages */
  private def handleWelcome(promise: Promise[Session]): Receive = {
    case msg: Welcome =>
      log.debug("<-- {}", msg)
      val session = new Session(this, msg)(system, validator)
      become(session.handle)
      promise.success(session)
  }


  /** Handle an incoming ABORT messages */
  private[client] def handleAbort(promise: Promise[Session]): Receive = {
    case message: Abort =>
      log.debug("<-- {}", message)
      promise.failure(new AbortException(message))
  }

  
  /** Handle an incoming GOODBYE messages */
  private[client] def handleGoodbye(session: Session): Receive = {
    case message: Goodbye =>
      log.debug("<-- {}", message)
      /*
       * A session ends when the underlying transport disappears or 
       * when it is closed explicitly by a GOODBYE message sent by 
       * one peer and a GOODBYE message sent from the other peer 
       * in response.
       */
      val reply = Goodbye(Goodbye.defaultDetails, "wamp.error.goodbye_and_out")
      transportHandler.tell(reply, clientRef)
      session.doClose()
  }
  
  /** Handle disconnection from router side */
  private[client] def handleDisconnected[T](promise: Promise[T]): Receive = {
    case signal @ Disconnected =>
      log.debug("!!! {}", signal)
      connected = false
      promise.failure(new TransportException("Disconnected from router side"))
  }
}


