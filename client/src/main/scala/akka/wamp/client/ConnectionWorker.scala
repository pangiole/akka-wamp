package akka.wamp.client

import java.net.URI
import java.util.concurrent.ThreadLocalRandom

import akka.actor.{ActorLogging, ActorRef, PoisonPill, Props}
import akka.io.IO
import akka.stream.StreamTcpException
import akka.wamp._
import akka.wamp.messages._
import akka.wamp.serialization.Payload

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

/**
  * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  * THE MOST IMPORTANT CLASS of AKKA WAMP CLIENT
  * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  */
private[client] 
class ConnectionWorker(address: URI, format: String, options: BackoffOptions, promise: Promise[Connection])
  extends ClientActor with ActorLogging
{
  import ConnectionWorker._

  private val manager = IO(Wamp)
  private var conn: Connection = _
  private var idgen: IdGenerator = _
  private var attemptCount = 0
  private var router: ActorRef = _

  private val pendingSubscribes: mutable.Map[Id, PendingSubscribe] = mutable.Map()
  private val subscriptions: mutable.Map[Id, Subscription] = mutable.Map()
  private val pendingUnsubscribes: mutable.Map[Id, PendingUnsubscribe] = mutable.Map()

  private val pendingPublications: mutable.Map[Id, PendingPublish] = mutable.Map()

  private val pendingRegisters: mutable.Map[Id, PendingRegister] = mutable.Map()
  private val registrations: mutable.Map[Id, Registration] = mutable.Map()
  private val pendingUnregisters: mutable.Map[Id, PendingUnregister] = mutable.Map()

  private val pendingCalls: mutable.Map[Id, PendingCall] = mutable.Map()


  @throws(classOf[Exception])
  override def preStart(): Unit = {
    log.debug(s"=== preStart")
    connect()
  }


  private def connect(): Unit = {
    context.system.scheduler.scheduleOnce(delay, manager, Connect(address, format))
    attemptCount += 1
  }


  override def receive: Receive = connecting


  /* CONNECTING state */
  private def connecting: Receive = {
    case sig @ CommandFailed(Connect(_, _), cause) =>
      log.debug("!!! ConnectFailed")
      cause match {
        case _: StreamTcpException =>
          connect()
        case _ =>
          // Unrecoverable exception. Promise fails and this actor stops.
          val ex = new ClientException(cause.getMessage, cause)
          promise.tryFailure(ex)
          throw ex
      }

    case sig @ Connected(handler, url, format) =>
      log.debug("=== Connected(handler = {})", sig)
      router = handler
      attemptCount = 0
      conn = new Connection(self, url, format)
      context become connected
      // WARN: share the connection delegate with the client via the promise
      promise.trySuccess(conn)

    case msg =>
      promise.tryFailure(new ClientException(s"Unexpected $msg", new IllegalStateException))
  }



  /* CONNECTED state */
  private def connected: Receive = {
    case sig @ Disconnected =>
      onRouterDisconnected()

    case cmd @ SendDisconnect(promise) =>
      sendDisconnect(promise)

    case cmd @ SendHello(realm, promise) =>
      try {
        context become opening(realm, promise)
        val msg = Hello(realm, Hello.defaultDetails) // could throw exception because of realm
        log.debug("--> {}", msg)
        router ! msg
      } catch {
        case cause: Throwable =>
          promise.failure(new ClientException(cause.getMessage, cause))
      }

    case msg =>
      promise.tryFailure(new ClientException(s"Unexpected $msg", new IllegalStateException))
  }


  /* OPENING state */
  private def opening(realm: Uri, promise: Promise[Session]): Receive = {
    case sig @ Disconnected =>
      onRouterDisconnected()
      promise.tryFailure(new ClientException("Disconnected", null))

    case msg: Abort =>
      log.debug("<-- {}", msg)
      promise.failure(new ClientException(msg.reason, null))

    case msg: Welcome =>
      log.debug("<-- {}", msg)
      conn.session = new Session(self, realm, msg)
      idgen = new SessionScopedIdGenerator
      context become open
      promise.success(conn.session)

    case msg =>
      promise.tryFailure(new ClientException(s"Unexpected $msg", new IllegalStateException))
  }


  /* OPEN state */
  private def open: Receive = {
    handleSession orElse
      handlePublications orElse
      handleSubscriptions orElse
      handleEvents orElse
      handleCalls orElse
      handleRegistrations orElse
      handleInvocations
  }


  /* Handle session in OPEN state */
  private def handleSession: Receive = {

    case sig @ Disconnected =>
      onRouterDisconnected()

    case cmd @ SendDisconnect(promise) =>
      sendDisconnect(promise)

    case cmd @ SendGoodbye(promise) =>
      context become closing(promise)
      router ! Goodbye(Goodbye.defaultDetails, Goodbye.defaultReason)

    case msg: Goodbye =>
      // session closing initiated by router
      log.debug("<-- {}", msg)
      conn.session.closed = true
      context become connected
      router ! Goodbye(Goodbye.defaultDetails, "wamp.error.goodbye_and_out")

    // TODO case msg => throw new SessionException(s"Unexpected $msg", new IllegalStateException)
  }


  /* Handle subscription in OPEN state */
  private def handleSubscriptions: Receive = {

    case cmd @ SendSubscribe(topic, consumer, promise) =>
      val req = Subscribe(idgen.nextId(), Subscribe.defaultOptions, topic)
      pendingSubscribes += (req.requestId -> PendingSubscribe(req, consumer, promise))
      log.debug("--> {}", req)
      router ! req

    case ack @ Subscribed(requestId, subscriptionId) =>
      log.debug("<-- {}", ack)
      pendingSubscribes.get(requestId).map {
        case PendingSubscribe(request, consumer,  promise) =>
          val subscription = new Subscription(request.topic, conn.session, consumer, ack)
          subscriptions += (subscriptionId -> subscription)
          pendingSubscribes -= requestId
          promise.success(subscription)
      }

    case err @ Error(Subscribe.tpe, requestId, _, error, _) =>
      log.debug("<-- {}", err)
      pendingSubscribes.get(requestId).map { pending =>
        pendingSubscribes -= requestId
        pending.promise.failure(new ClientException(error, null))
      }

    case cmd @ SendUnsubscribe(subscriptionId, promise) =>
      val req = Unsubscribe(idgen.nextId(), subscriptionId)
      pendingUnsubscribes += (req.requestId -> PendingUnsubscribe(req, promise))
      router ! req

    case ack @ Unsubscribed(requestId) =>
      log.debug("<-- {}", ack)
      pendingUnsubscribes.get(requestId).map {
        case PendingUnsubscribe(Unsubscribe(_, subscriptionId), promise) =>
          subscriptions -= subscriptionId
          pendingUnsubscribes -= requestId
          promise.success(ack)
      }

    case err @ Error(Unsubscribe.tpe, requestId, _, error, _) =>
      log.debug("<-- {}", err)
      pendingUnsubscribes.get(requestId).map {
        case PendingUnsubscribe(_, promise) =>
          pendingUnsubscribes -= requestId
          promise.failure(new ClientException(error, null))
      }
  }


  /* Handle events in OPEN state */
  private def handleEvents: Receive = {

    case evt @ Event(subscriptionId, _, _, _) =>
      log.debug("<-- {}", evt)
      subscriptions.get(subscriptionId) match {
        case Some(sb) =>
          try {
            sb.consumer.apply(evt)
          } catch {
            case ex: Throwable =>
              log.warning("!!! {}: {}", ex.getClass.getName, ex.getMessage)
          }
        case None =>
          log.warning("!!! Unknown subscriptionId {}", subscriptionId)
      }
  }


  /* Handle publications in OPEN state */
  private def handlePublications: Receive = {

    case cmd @ SendPublish(topic, payload, None) =>
      val req = Publish(idgen.nextId(), Publish.defaultOptions, topic, payload)
      log.debug("--> {}", req)
      router ! req

    case cmd @ SendPublish(topic, payload, Some(promise)) =>
      val req = Publish(idgen.nextId(), Publish.defaultOptions.withAcknowledge(true), topic, payload)
      pendingPublications += (req.requestId -> PendingPublish(req, promise))
      log.debug("--> {}", req)
      router ! req

    case ack @ Published(requestId, _) =>
      log.debug("<-- {}", ack)
      pendingPublications.get(requestId).map {
        case PendingPublish(request, promise) =>
          pendingPublications -= requestId
          promise.success(new Publication(request.topic, ack))
      }

    case err @ Error(Publish.tpe, requestId, _, error, _) =>
      log.debug("<-- {}", err)
      pendingPublications.get(requestId).map {
        case PendingPublish(_, promise) =>
          pendingPublications -= requestId
          promise.failure(new ClientException(error, null))
      }
  }


  /* Handle registration in OPEN state */
  private def handleRegistrations: Receive = {

    case cmd @ SendRegister(procedure, handler, promise) =>
      val req = Register(idgen.nextId(), Register.defaultOptions, procedure)
      pendingRegisters += (req.requestId -> new PendingRegister(req, handler, promise))
      log.debug("--> {}", req)
      router ! req

    case ack @ Registered(requestId, registrationId) =>
      log.debug("<-- {}", ack)
      pendingRegisters.get(requestId).map {
        case PendingRegister(request, handler, promise) =>
          val registration = new Registration(request.procedure, conn.session, handler, ack)
          registrations += (registrationId -> registration)
          pendingRegisters -= requestId
          promise.success(registration)
        }

    case err @ Error(Register.tpe, requestId, _, error, _) =>
      log.debug("<-- {}", err)
      pendingRegisters.get(requestId).map { pending =>
        pendingRegisters -= requestId
        pending.promise.failure(new ClientException(error, null))
      }

    case cmd @ SendUnregister(registrationId, promise) =>
      val req = Unregister(idgen.nextId(), registrationId)
      pendingUnregisters += (req.requestId -> PendingUnregister(req, promise))
      router ! req

    case ack @ Unregistered(requestId) =>
      log.debug("<-- {}", ack)
      pendingUnregisters.get(requestId).map {
        case PendingUnregister(Unregister(_, registrationId), promise) =>
          registrations -= registrationId
          pendingUnregisters -= requestId
          promise.success(ack)
      }

    case err @ Error(Unregister.tpe, requestId, _, error, _) =>
      log.debug("<-- {}", err)
      pendingUnregisters.get(requestId).map {
        case PendingUnregister(_, promise) =>
          pendingUnregisters -= requestId
          promise.failure(new ClientException(error, null))
      }
  }



  /* Handle calls in OPEN state */
  private def handleCalls: Receive = {

    case cmd @ SendCall(procedure, payload, promise) =>
      val req = Call(idgen.nextId(), Call.defaultOptions, procedure, payload)
      pendingCalls += (req.requestId -> PendingCall(req, promise))
      log.debug("--> {}", req)
      router ! req

    case res @ Result(requestId, _, _) =>
      log.debug("<-- {}", res)
      pendingCalls.get(requestId).map {
        case PendingCall(_, promise) =>
          pendingCalls -= requestId
          promise.success(res)
      }

    case err @ Error(Call.tpe, requestId, _, error, _) =>
      log.debug("<-- {}", err)
      pendingCalls.get(requestId).map {
        case PendingCall(_, promise) =>
          pendingCalls -= requestId
          promise.failure(new ClientException(error, null))
      }
  }


  /* Handle invocation in OPEN state */
  private def handleInvocations: Receive = {

    case req @ Invocation(requestId, registrationId, _, _) =>
      log.debug("<-- {}", req)
      registrations.get(registrationId) match {
        case Some(registration) =>
          val f = Future(Payload(List(registration.handler.apply(req))))
          f.onComplete {
            case Success(p) =>
              router ! Yield(requestId, payload = p)
            case Failure(ex) =>
              router ! Error(Invocation.tpe, requestId, Error.defaultDict, "akka.wamp.error.callee_exception", Payload(List(ex.getMessage)))
          }
        case None =>
          log.warning("!!! Unknown registrationId {}", registrationId)
      }
  }


  /* CLOSING state (initiate by client) */
  private def closing(promise: Promise[Closed]): Receive = {

    case sig @ Disconnected =>
      onRouterDisconnected()
      promise.failure(new ClientException("Disconnected", null))

    case msg: Goodbye =>
      // upon receiving goodbye message FROM the router
      log.debug("<-- {}", msg)
      conn.session.closed = true
      conn.session = null
      promise.success(Closed)

    case msg =>
      promise.tryFailure(new ClientException(s"Unexpected $msg", new IllegalStateException))
  }


  /* DISCONNECTING state */
  private def disconnecting(promise: Promise[Disconnected]): Receive = {

    case sig @ Disconnected =>
      onRouterDisconnected()
      promise.success(sig)

    case msg =>
      promise.tryFailure(new ClientException(s"Unexpected $msg", new IllegalStateException))
  }



  private def onRouterDisconnected() = {
    log.debug("!!! RouterDisconnected !!!")
    conn.disconnected = true
    self ! PoisonPill
  }

  private def sendDisconnect(promise: Promise[Disconnected]) = {
    log.debug("!!! ClientDisconnecting ...")
    context become disconnecting(promise)
    router ! Disconnect
  }

  /* Calculate restart delay applying exponential backoff algorithm */
  private def delay(): FiniteDuration = {
    val rnd = 1.0 + ThreadLocalRandom.current().nextDouble() * options.randomFactor
    if (attemptCount >= 30) // Duration overflow protection (> 100 years)
      options.maxBackoff
    else
      options.maxBackoff.min(options.minBackoff * math.pow(2, attemptCount)) * rnd match {
        case f: FiniteDuration => f
        case _ => options.maxBackoff
      }
  }


  @throws(classOf[Exception])
  override def postStop(): Unit = {
    log.debug("    postStop")
    (
      pendingSubscribes.values ++
      pendingUnsubscribes.values ++
      pendingPublications.values ++
      pendingRegisters.values ++
      pendingCalls.values
    )
    .foreach(_.promise.failure(new ClientException("Connector stopped", null)))
  }
}


private[client] object ConnectionWorker {

  case class SendDisconnect(promise: Promise[Disconnected]) extends Command
  case class SendHello(realm: Uri, promise: Promise[Session]) extends Command
  case class SendGoodbye(promise: Promise[Closed]) extends Command
  case class SendSubscribe(topic: String, consumer: (Event) => Unit, promise: Promise[Subscription]) extends Command
  case class SendUnsubscribe(subscriptionId: Id, promise: Promise[Unsubscribed]) extends Command
  case class SendPublish(topic: Uri, payload: Payload, promise: Option[Promise[Publication]]) extends Command
  case class SendRegister(procedure: Uri, handler: (Invocation) => Any, promise: Promise[Registration]) extends Command
  case class SendUnregister(registrationId: Id, promise: Promise[Unregistered]) extends Command
  case class SendCall(procedure: Uri, payload: Payload, promise: Promise[Result]) extends Command

  trait PendingRequest { type T; val promise: Promise[T] }

  case class PendingSubscribe(request: Subscribe, consumer: (Event) => Unit, val promise: Promise[Subscription]) extends PendingRequest { type T = Subscription }
  case class PendingUnsubscribe(request: Unsubscribe, val promise: Promise[Unsubscribed]) extends PendingRequest { type T = Unsubscribed }
  case class PendingPublish(request: Publish, val promise: Promise[Publication]) extends PendingRequest { type T = Publication }
  case class PendingRegister(request: Register, handler: (Invocation) => Any, val promise: Promise[Registration]) extends PendingRequest { type T = Registration }
  case class PendingUnregister(request: Unregister, val promise: Promise[Unregistered]) extends PendingRequest { type T = Unregistered }
  case class PendingCall(request: Call, val promise: Promise[Result]) extends PendingRequest { type T = Result }

  def props(address: URI, format: String, options: BackoffOptions, promise: Promise[Connection]) =
    Props(new ConnectionWorker(address, format, options, promise))
}