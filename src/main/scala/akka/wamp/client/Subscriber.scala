package akka.wamp.client

import akka.actor.Actor._
import akka.wamp._
import akka.wamp.messages._

import scala.collection.mutable
import scala.concurrent._

/**
  * Subscriber is a client that subscribes to topics and
  * expects to receive events
  */
trait Subscriber { this: Session =>

  private val pendingSubscriptions: PendingSubscriptions = mutable.Map()
  
  private val subscriptions: Subscriptions = mutable.Map()

  private val pendingUnsubscribes: PendingUnsubscribes = mutable.Map()
  
  private val eventHandlers: EventHandlers = mutable.Map()
  

  /**
    * Subscribe to the given topic so that the given handler will be 
    * executed on events.
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
    * @param topic is the topic the subscriber wants to subscribe to
    * @param options is the option dictionary (default is empty)
    * @param handler is the handler executed on events
    * @return the (future of) subscription 
    */
  def subscribe(topic: Uri, options: Dict = Subscribe.defaultOptions)(handler: EventHandler): Future[Subscription] = {
    withPromise[Subscription] { promise =>
      val msg = Subscribe(requestId = nextId(), options, topic)
      pendingSubscriptions += (msg.requestId -> PendingSubscription(msg, handler, promise))
      connection ! msg
    }
  }

  protected def handleSubscriptionSuccess: Receive = {
    case msg @ Subscribed(requestId, subscriptionId) =>
      log.debug("<-- {}", msg)
      pendingSubscriptions.get(requestId).map { pending =>
          val subscription = Subscription(pending.subscribe.topic, msg)
          subscriptions += (subscriptionId -> subscription)
          eventHandlers += (subscriptionId -> pending.handler)
          pendingSubscriptions -= requestId
          pending.promise.success(subscription)
      }
  }

  protected def handleSubscriptionError: Receive = {
    case msg @ Error(Subscribe.tpe, requestId, _, error, _) =>
      log.debug("<-- {}", msg)
      pendingSubscriptions.get(requestId).map { pending =>
        pendingSubscriptions -= requestId
        pending.promise.failure(new SessionException(error))
      }
  }


  // ~~~~~~~~~~~~~~~~~~~~~~~
  
  
  /**
    * Unsubscribe from the given topic
    *
    * @param topic is the topic to unsubscribe from
    * @return a (future of) unsubscribed
    */
  def unsubscribe(topic: Uri): Future[Unsubscribed] = {
    subscriptions.find { case (_, subscription) =>  subscription.topic == topic } match {
      case Some((subscriptionId, _)) => {
        withPromise[Unsubscribed] { promise =>
          val msg = Unsubscribe(requestId = nextId(), subscriptionId)
          pendingUnsubscribes += (msg.requestId -> (msg, promise))
          connection ! msg
        }
      }
      case None =>
        Future.failed[Unsubscribed](new SessionException("akka.wamp.error.no_such_topic"))
    }
  }

  protected def handleUnsubscribed: Receive = {
    case msg @ Unsubscribed(requestId) => 
      log.debug("<-- {}", msg)
      pendingUnsubscribes.get(requestId).map {
        case (Unsubscribe(_, subscriptionId), promise) => 
          subscriptions -= subscriptionId
          eventHandlers -= subscriptionId
          pendingUnsubscribes -= requestId
          promise.success(msg)
      }
  }

  protected def handleUnsubscribedError: Receive = {
    case msg @ Error(Subscribe.tpe, requestId, _, error, _) =>
      log.debug("<-- {}", msg)
      pendingUnsubscribes.get(requestId).map {
        case (_, promise) =>
          pendingUnsubscribes -= requestId
          promise.failure(new SessionException(error))
      }
  }


  // ~~~~~~~~~~~~~~~~~~~~~~~
  
  
  protected def handleEvent: Receive = {
    case msg @ Event(subscriptionId, _, _, _) =>
      log.debug("<-- {}", msg)
      eventHandlers.get(subscriptionId) match {
        case Some(handler) => handler(msg)
        case None => log.warn("!!! event handler not found for subscriptionId {}", subscriptionId)
      }
  }
}

object Subscriber



case class Subscription(topic: Uri, subscribed: Subscribed)

case class PendingSubscription(subscribe: Subscribe, handler: EventHandler, promise: Promise[Subscription])

