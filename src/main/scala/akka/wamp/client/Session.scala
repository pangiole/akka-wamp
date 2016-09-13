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
  *   val client = Client()
  *   val session = for {
  *     conn <- client.connect("ws://host:9999", "wamp.2.json")
  *     ssn <- conn.open("myapp.realm", Set(Roles.publisher))
  *   } yield ssn
  * }}}
  * 
  * A session is joined to a realm on a router. Routing occurs only
  * between WAMP sessions that have joined the same realm.
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
  
  private var subscriptions = mutable.Map.empty[Id, Promise[Subscribed]]

  private var eventHandlers = mutable.Map.empty[Id, EventHandler]

  private var publications = mutable.Map.empty[Id, Promise[Either[Done, Published]]]

  private var registrations = mutable.Map.empty[Id, Promise[Registered]]

  private var invocationHandlers = mutable.Map.empty[Id, InvocationHandler]
  
  private var closed = false 
  
  private[client] def doClose(): Unit = {
    closed = true
  }
  
  private[client] def futureOf[T](fn: (Promise[T]) => Unit): Future[T] = {
    val promise = Promise[T]
    if (closed) {
      promise.failure(SessionException("session closed"))
    }
    else {
      fn(promise)
    }
    promise.future
  }
  
  
  def close(reason: Uri = Goodbye.defaultReason, details: Dict = Goodbye.defaultDetails): Future[Goodbye] = {
    futureOf[Goodbye] { promise =>
      def goodbyeReceive: Receive = {
        case msg: Goodbye =>
          log.debug("<-- {}", msg)
          promise.success(msg)
      }
      conn.become(
        goodbyeReceive orElse
          handleUnexpected(promise = Some(promise))
      )
      conn ! Goodbye(details, reason)
    }
  }
  
  
  
  def subscribe(topic: Uri, options: Dict = Subscribe.defaultOptions)(handler: EventHandler): Future[Subscribed] = {
    futureOf[Subscribed] { promise =>
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
    * Publish to the given topic 
    * 
    * @param topic is the topic to publish to
    * @param ack is the acknowledge boolean become (default is ``false``)
    * @param payload is the (option of) payload (default is None)
    * @return
    */
  def publish(topic: Uri, ack: Boolean = false, payload: Option[Payload] = None): Future[Either[Done, Published]] =  {
    futureOf[Either[Done, Published]] { promise =>
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
    futureOf[Registered] { promise =>
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

