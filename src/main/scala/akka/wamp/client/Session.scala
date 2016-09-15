package akka.wamp.client

import akka.Done
import akka.actor.Actor.Receive
import akka.wamp._
import akka.wamp.messages._
import akka.wamp.serialization._
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.{Future, Promise}

/**
  * WAMP sessions are established over a WAMP connection.
  * 
  * {{{
  *   import akka.actor._
  *   import akka.wamp.client._
  *
  *   implicit val system = ActorSystem("myapp")
  *   implicit val ec = system.dispatcher
  *   
  *   val client = Client()
  *   val session: Future[Session] = 
  *     client.connect(
  *       url = "ws://host:9999/router", 
  *       subprotocol = "wamp.2.json"
  *     )
  *     .flatMap(_.openSession(
  *       realm = "myapp.realm", 
  *       roles = Set(Roles.publisher)
  *     ))
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
  * @param conn is the WAMP connection
  * @param welcome is the WELCOME message
  * @param validator is WAMP types validator
  */
class Session private[client](conn: Connection, welcome: Welcome)(implicit validator: Validator) 
  extends SessionLike 
    with Scope.Session 
{
  import conn.{handleGoodbye, handleUnexpected}
  
  private val log = LoggerFactory.getLogger(classOf[Connection])

  /**
    * This session identifier
    */
  val id = welcome.sessionId

  /**
    * This session details
    */
  val details = welcome.details
  
  /** The subscriptions map */
  private var subscriptions = mutable.Map.empty[Id, Promise[Subscribed]]

  /** The event handler map */
  private var eventHandlers = mutable.Map.empty[Id, EventHandler]

  /** The event publications map */
  private var publications = mutable.Map.empty[Id, Promise[Either[Done, Published]]]

  /** The procedure registrations map */
  private var registrations = mutable.Map.empty[Id, Promise[Registered]]

  /** The procedure invocations map */
  private var invocationHandlers = mutable.Map.empty[Id, InvocationHandler]
  
  /** The boolean switch to determine if this session is closed */
  private var closed = false

  /**
    * If this session is closed
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
    * @return the (future of) message replied by the router
    */
  def close(reason: Uri = Goodbye.defaultReason, details: Dict = Goodbye.defaultDetails): Future[Goodbye] = {
    withPromise[Goodbye] { promise =>
      def handleGoodbye: Receive = {
        case msg: Goodbye =>
          // upon receiving goodbye message FROM the router 
          log.debug("<-- {}", msg)
          doClose()
          promise.success(msg)
      }
      conn.become(
        handleGoodbye orElse
          handleUnexpected(Some(promise))
      )
      // send goodbye message TO the router
      conn ! Goodbye(details, reason)
    }
  }
  
  
  // TODO def onClose() = { /* when the router sends GOODBYE */ }


  /**
    * Subscribe to the given topic so that the given handler will be 
    * triggered on events.
    * 
    * {{{
    *   ,---------.          ,------.             ,----------.
    *   |Publisher|          |Broker|             |Subscriber|
    *   `----+----'          `--+---'             `----+-----'
    *        |                  |                      |
    *        |                  |                      |
    *        |                  |       SUBSCRIBE      |
    *        |                  | <---------------------
    *        |                  |                      |
    *        |                  |  SUBSCRIBED or ERROR |
    *        |                  | --------------------->
    *        |                  |                      |
    *        |                  |                      |
    *        |                  |                      |
    *        |                  |                      |
    *        |                  |      UNSUBSCRIBE     |
    *        |                  | <---------------------
    *        |                  |                      |
    *        |                  | UNSUBSCRIBED or ERROR|
    *        |                  | --------------------->
    *   ,----+----.          ,--+---.             ,----+-----.
    *   |Publisher|          |Broker|             |Subscriber|
    *   `---------'          `------'             `----------'
    * }}}
    * 
    * @param topic is the topic to subscribe to
    * @param options is the option dictionary (default is empty)
    * @param handler is the handler to trigger on events
    * @return the (future of) subscription 
    */
  def subscribe(topic: Uri, options: Dict = Subscribe.defaultOptions)(handler: EventHandler): Future[Subscribed] = {
    withPromise[Subscribed] { promise =>
      val msg = Subscribe(requestId = nextId(), options, topic)
      subscriptions += (msg.requestId -> promise)
      conn.become(
        handleGoodbye(session = this) orElse
          handleError(subscriptions) orElse
            handleSubscribed(subscriptions, handler) orElse
              handleEvent(eventHandlers) orElse
                handleUnexpected(promise = Some(promise))
      )
      conn ! msg
    }
  }


  /**
    * Publish an event to the given topic with the given (option of) payload
    * 
    * {{{
    *    ,---------.          ,------.          ,----------.
    *   |Publisher|          |Broker|          |Subscriber|
    *   `----+----'          `--+---'          `----+-----'
    *        |     PUBLISH      |                   |
    *        |------------------>                   |
    *        |                  |                   |
    *        |PUBLISHED or ERROR|                   |
    *        |<------------------                   |
    *        |                  |                   |
    *        |                  |       EVENT       |
    *        |                  | ------------------>
    *   ,----+----.          ,--+---.          ,----+-----.
    *   |Publisher|          |Broker|          |Subscriber|
    *   `---------'          `------'          `----------'
    * }}}
    * 
    * @param topic is the topic to publish to
    * @param ack is the acknowledge boolean switch (default is ``false``)
    * @param payload is the (option of) payload (default is ``None``)
    * @return either done or a (future of) publication 
    */
  def publish(topic: Uri, ack: Boolean = false, payload: Option[Payload] = None): Future[Either[Done, Published]] =  {
    withPromise[Either[Done, Published]] { promise =>
      val message = Publish(requestId = nextId(), Dict().withAcknowledge(ack), topic, payload)
      publications += (message.requestId -> promise)
      if (!ack) {
        conn ! message
        promise.success(Left(Done))
      }
      else {
        conn.become(
          handleGoodbye(session = this) orElse
            handleError(publications) orElse
              handlePublished(publications) orElse
                handleUnexpected(promise = Some(promise))
        )
        conn ! message
      } 
    }
  }

  
  def register(procedure: Uri, options: Dict = Register.defaultOptions)(handler: InvocationHandler): Future[Registered] = {
    withPromise[Registered] { promise =>
      val msg = Register(requestId = nextId(), options, procedure)
      registrations += (msg.requestId -> promise)
      conn.become(
        handleGoodbye(session = this) orElse
          handleError(registrations) orElse
          handleRegistered(registrations, handler) orElse
          handleInvocation(invocationHandlers) orElse
          handleUnexpected(promise = Some(promise))
      )
      conn ! msg
    }
  }
  
  
  
  private[client] def handleError[T](promises: mutable.Map[Id, Promise[T]]): Receive = {
    case msg: Error =>
      log.debug("<-- {}", msg)
      if (promises.isDefinedAt(msg.requestId)) {
        promises(msg.requestId).failure(new Throwable(msg.toString()))
        promises - msg.requestId
      }
  }
  

  private[client] def handleSubscribed(promises: mutable.Map[Id, Promise[Subscribed]], handler: EventHandler): Receive = {
    case msg: Subscribed =>
      log.debug("<-- {}", msg)
      promises.get(msg.requestId).map { p =>
        eventHandlers += (msg.subscriptionId -> handler)
        p.success(msg)
        promises - msg.requestId
      }
  }

  private[client] def handlePublished(promises: mutable.Map[Id, Promise[Either[Done, Published]]]): Receive = {
    case msg: Published =>
      log.debug("<-- {}", msg)
      promises.get(msg.requestId).map { p =>
        p.success(Right(msg))
        promises - msg.requestId
      }
  }

  private[client] def handleEvent(handlers: mutable.Map[Id, EventHandler]): Receive = {
    case event: Event =>
      log.debug("<-- {}", event)
      handlers.get(event.subscriptionId).map(_(event))
  }

  private[client] def handleRegistered(promises: mutable.Map[Id, Promise[Registered]], handler: InvocationHandler): Receive = {
    case msg: Registered =>
      log.debug("<-- {}", msg)
      promises.get(msg.requestId).map { p =>
        invocationHandlers += (msg.registrationId -> handler)
        p.success(msg)
        promises - msg.requestId
      }
  }

  
  // TODO private[client] def handleCall

  
  private[client] def handleInvocation(handlers: mutable.Map[Id, InvocationHandler]): Receive = {
    case invocation: Invocation =>
      log.debug("<-- {}", invocation)
      handlers.get(invocation.requestId).map(_(invocation))
  }
}

