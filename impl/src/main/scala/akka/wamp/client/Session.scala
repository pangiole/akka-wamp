package akka.wamp.client

import akka.actor.Actor.Receive
import akka.actor.{ActorSystem, Cancellable}
import akka.wamp._
import akka.wamp.messages._
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * WAMP sessions are established over a WAMP connection.
  * 
  * {{{
  *   import akka.wamp.client._
  *   val client = Client("myapp")
  *
  *   import scala.concurrent.Future
  *   implicit val ec = client.executionContext
  *   
  *   val session: Future[Session] = 
  *     client.openSession(
  *       url = "ws://localhost:8080/ws", 
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
class Session private[client](val connection: Transport, welcome: Welcome)
                             (implicit val system: ActorSystem, val validator: Validator) 
  extends SessionLike 
    with Subscriber 
    with Publisher 
    with Callee
    with Caller
    with Scope.SessionScope 
{
  import connection.handleGoodbye

  implicit val ec = system.dispatcher
  
  protected val log = LoggerFactory.getLogger(classOf[Transport])

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
  def close(reason: Uri = Goodbye.defaultReason, details: Dict = Goodbye.defaultDetails): Future[Transport] = {
    withPromise[Transport] { promise =>
      def handleGoodbye: Receive = {
        case msg: Goodbye =>
          // upon receiving goodbye message FROM the router 
          log.debug("<-- {}", msg)
          doClose()
          promise.success(connection)
      }
      connection.become(
        handleGoodbye
      )
      // send goodbye message TO the router
      connection ! Goodbye(details, reason)
    }
    // TODO dispose all pending promises
  }

  /**
    * Schedules a task to be run repeatedly with an initial delay and
    * a frequency.
    * 
    * @param initialDelay
    * @param interval
    * @param task
    * @param executor
    * @return
    */
  def schedule[R](initialDelay: FiniteDuration, interval: FiniteDuration, task: (Unit) => R)
              (implicit executor: ExecutionContext): Cancellable = {
    this.system.scheduler.schedule(initialDelay, interval, new Runnable {
      override def run(): Unit = task(); ()
    })
  }
  
  
  // TODO def onClose() = { /* when the router sends GOODBYE */ }


  def handle: Receive = {
    handleGoodbye(session = this) orElse
      handleSubscriptions orElse 
      handlePublications orElse 
      handleEvents orElse
      handleRegistrations orElse 
      handleInvocations orElse
      handleResults
  }
}

