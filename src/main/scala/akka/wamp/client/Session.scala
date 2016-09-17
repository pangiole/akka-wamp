package akka.wamp.client

import akka.actor.Actor.Receive
import akka.wamp._
import akka.wamp.messages._
import org.slf4j.LoggerFactory

import scala.concurrent.{Future, Promise}

/**
  * WAMP sessions are established over a WAMP connection.
  * 
  * {{{
  *   import akka.wamp.client._
  *   val client = Client("myapp")
  *
  *   import scala.concurrent.Future
  *   import client.executionContext
  *   
  *   val session: Future[Session] = 
  *     client.openSession(
  *       url = "ws://localhost:8080/router", 
  *       subprotocol = "wamp.2.json",
  *       realm = "myapp.realm", 
  *       roles = Set(Roles.publisher)
  *     )
  * }}}
  * 
  * A session is joined to a realm on a router. Routing occurs only
  * between WAMP sessions that have joined the same realm.
  * 
  * Once got a session, it allows to publish/subscribe events 
  * and register/call procedures.
  * 
  * {{{
  *   val handler: EventHandler = { event =>
  *     event.payload.map(p => p.args
  *   }
  *   val subscription: Future[Subscribed] = 
  *     session.flatMap(
  *       _.subscribe(
  *         topic = "myapp.topic",
  *         options = Dict()
  *       )(handler))
  * }}}
  * 
  * @param connection is the WAMP connection
  * @param welcome is the WELCOME message
  * @param validator is WAMP types validator
  */
class Session private[client](val connection: Connection, welcome: Welcome)(implicit val validator: Validator) 
  extends SessionLike with Subscriber with Publisher with Callee
    with Scope.Session 
{
  import connection.{handleGoodbye, handleUnexpected}
  
  protected val log = LoggerFactory.getLogger(classOf[Connection])

  /**
    * This session identifier
    */
  val id = welcome.sessionId

  /**
    * This session details
    */
  val details = welcome.details
  
  /** The boolean switch to determine if this session is closed */
  private var closed = false

  /**
    * If this session is closed
    *
    * @return true or false
    */
  def isClosed = closed
  
  /** Unset the boolean close switch */
  private[client] def doClose(): Unit = {
    closed = true
  }
  
  /** Check the boolean close switch */
  private[client] def withPromise[T](fn: (Promise[T]) => Unit): Future[T] = {
    val promise = Promise[T]
    if (closed) promise.failure(SessionException("session closed"))
    else fn(promise)
    promise.future
  }


  /**
    * Close this session with the given reason and detail.
    * 
    * The client sends a GOODBYE message to the router which will 
    * in turn replies with a GOODBYE message.
    * 
    * {{{
    *     ,------.          ,------.
    *     |Client|          |Router|
    *     `--+---'          `--+---'
    *        |     GOODBYE     |
    *        | ---------------->
    *        |                 |
    *        |     GOODBYE     |
    *        | <----------------
    *     ,--+---.          ,--+---.
    *     |Client|          |Router|
    *     `------'          `------'
    * }}}
    * 
    * @param reason is the reason to close (default is ``wamp.error.close_realm``)
    * @param details are the details to send (default is empty)
    * @return the (future of) connection
    */
  def close(reason: Uri = Goodbye.defaultReason, details: Dict = Goodbye.defaultDetails): Future[Connection] = {
    withPromise[Connection] { promise =>
      def handleGoodbye: Receive = {
        case msg: Goodbye =>
          // upon receiving goodbye message FROM the router 
          log.debug("<-- {}", msg)
          doClose()
          promise.success(connection)
      }
      connection.become(
        handleGoodbye orElse 
        handleUnexpected
      )
      // send goodbye message TO the router
      connection ! Goodbye(details, reason)
    }
    // TODO dispose all pending promises
  }
  
  
  // TODO def onClose() = { /* when the router sends GOODBYE */ }
  
  
  def handle: Receive = {
    handleGoodbye(session = this) orElse
      handleSubscriptionSuccess orElse
      handleSubscriptionError orElse
      handleUnsubscribed orElse 
      handleUnsubscribedError orElse
      handlePublicationSuccess orElse
      handlePublicationError orElse
      handleEvent orElse
      handleRegistrationSuccess orElse
      handleRegistrationError orElse
      // TODO handleUnregisterSuccess orElse handleUnregisterError 
      handleInvocation orElse
      handleUnexpected
  }

}

