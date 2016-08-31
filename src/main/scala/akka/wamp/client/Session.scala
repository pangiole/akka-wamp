package akka.wamp.client

import akka.Done
import akka.actor.Actor.Receive
import akka.wamp._
import akka.wamp.messages._
import akka.wamp.serialization._
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.{Future, Promise}

class Session private[client](transport: Transport, welcome: Welcome)(implicit val validator: Validator) 
  extends SessionLike 
    with Scope.Session 
{
  import transport.{goodbyeReceive, unexpectedReceive}
  
  private val log = LoggerFactory.getLogger(classOf[Transport])

  val id = welcome.sessionId

  val details = welcome.details
  
  private var subscriptions = mutable.Map.empty[Id, Promise[Subscribed]]
  
  private var publications = mutable.Map.empty[Id, Promise[Either[Done, Published]]]
  
  private var eventHandlers = mutable.Map.empty[Id, EventHandler]
  
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
      transport.become(
        goodbyeReceive orElse
          unexpectedReceive(promiseToBreak = Some(promise))
      )
      transport ! Goodbye(details, reason)
    }
  }
  
  
  
  def subscribe(topic: Uri, options: Dict = Subscribe.defaultOptions)(handler: EventHandler): Future[Subscribed] = {
    futureOf[Subscribed] { promise =>
      val msg = Subscribe(nextId(), options, topic)
      subscriptions += (msg.requestId -> promise)
      transport.become(
        goodbyeReceive(session = this) orElse
          errorReceive(subscriptions) orElse
            subscribedReceive(subscriptions, handler) orElse
              eventReceive(eventHandlers) orElse
                unexpectedReceive(promiseToBreak = Some(promise))
      )
      transport ! msg
    }
  }


  /**
    * Publish to the given topic 
    * 
    * @param topic is the topic to publish to
    * @param acknowledge is the acknowledge boolean become (default is ``false``)
    * @param payload is the (option of) payload (default is None)
    * @return
    */
  def publish(topic: Uri, acknowledge: Boolean = false, payload: Option[Payload] = None): Future[Either[Done, Published]] =  {
    futureOf[Either[Done, Published]] { promise =>
      val message = Publish(nextId(), Dict().withAcknowledge(acknowledge), topic, payload)
      publications += (message.requestId -> promise)
      if (!acknowledge) {
        transport ! message
        promise.success(Left(Done))
      }
      else {
        transport.become(
          goodbyeReceive(session = this) orElse
            errorReceive(publications) orElse
              publishedReceive(publications) orElse
                unexpectedReceive(promiseToBreak = Some(promise))
        )
        transport ! message
      } 
    }
  }

  
  
  
  private[client] def errorReceive[T](promises: mutable.Map[Id, Promise[T]]): Receive = {
    case msg: Error =>
      log.debug("<-- {}", msg)
      if (promises.isDefinedAt(msg.requestId)) {
        promises(msg.requestId).failure(new Throwable(msg.toString()))
        promises - msg.requestId
      }
  }
  
  private[client] def publishedReceive(promises: mutable.Map[Id, Promise[Either[Done, Published]]]): Receive = {
    case msg: Published =>
      log.debug("<-- {}", msg)
      promises.get(msg.requestId).map { p =>
        p.success(Right(msg))
        promises - msg.requestId
      }
  }

  private[client] def subscribedReceive(promises: mutable.Map[Id, Promise[Subscribed]], handler: (Event) => Unit): Receive = {
    case msg: Subscribed =>
      log.debug("<-- {}", msg)
      promises.get(msg.requestId).map { p =>
        eventHandlers += (msg.subscriptionId -> handler)
        p.success(msg)
        promises - msg.requestId
      }
  }

  private[client] def eventReceive(handlers: mutable.Map[Id, EventHandler]): Receive = {
    case event: Event =>
      log.debug("<-- {}", event)
      handlers.get(event.subscriptionId).map(_(event))
  }
}

