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
  import Subscriber._

  private val pendingSubscribers: mutable.Map[RequestId, PendingSubscription] = mutable.Map()
  
  private val subscriptions: mutable.Map[SubscriptionId, Subscription] = mutable.Map()

  private val pendingUnsubscribes: mutable.Map[RequestId, (Unsubscribe, Promise[Unsubscribed])] = mutable.Map()
  
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
    * @param handler is the handler executed on events
    * @return the (future of) subscription 
    */
  def subscribe(topic: Uri)(handler: EventHandler): Future[Subscription] = {
    withPromise[Subscription] { promise =>
      val msg = Subscribe(requestId = nextRequestId(), Subscribe.defaultOptions, topic)
      pendingSubscribers += (msg.requestId -> new PendingSubscription(msg, handler, promise))
      connection ! msg
    }
  }


  /**
    * Unsubscribe from the given topic
    *
    * @param topic is the topic to unsubscribe from
    * @return a (future of) unsubscribed
    */
  def unsubscribe(topic: Uri): Future[Unsubscribed] = {
    withPromise[Unsubscribed] { promise =>
      subscriptions.find { case (_, subscription) =>  subscription.topic == topic } match {
        case Some((subscriptionId, _)) => {
          val msg = Unsubscribe(requestId = nextRequestId(), subscriptionId)
          pendingUnsubscribes += (msg.requestId -> (msg, promise))
          connection ! msg
        }
        case None =>
          Future.failed[Unsubscribed](new SessionException("akka.wamp.error.no_such_topic"))
      }
    }
  }

  
  

  protected def handleSubscriptions: Receive = {
    case msg @ Subscribed(requestId, subscriptionId) =>
      log.debug("<-- {}", msg)
      pendingSubscribers.get(requestId).map { pending =>
          val subscription = new Subscription(this, pending.subscribe.topic, msg)
          subscriptions += (subscriptionId -> subscription)
          eventHandlers += (subscriptionId -> pending.handler)
          pendingSubscribers -= requestId
          pending.promise.success(subscription)
      }
      
    case msg @ Error(Subscribe.tpe, requestId, _, error, _) =>
      log.debug("<-- {}", msg)
      pendingSubscribers.get(requestId).map { pending =>
        pendingSubscribers -= requestId
        pending.promise.failure(new SessionException(error))
      }
      
    case msg @ Unsubscribed(requestId) =>
      log.debug("<-- {}", msg)
      pendingUnsubscribes.get(requestId).map {
        case (Unsubscribe(_, subscriptionId), promise) =>
          subscriptions -= subscriptionId
          eventHandlers -= subscriptionId
          pendingUnsubscribes -= requestId
          promise.success(msg)
      }

    case msg @ Error(Unsubscribe.tpe, requestId, _, error, _) =>
      log.debug("<-- {}", msg)
      pendingUnsubscribes.get(requestId).map {
        case (_, promise) =>
          pendingUnsubscribes -= requestId
          promise.failure(new SessionException(error))
      }
  }


  // ~~~~~~~~~~~~~~~~~~~~~~~
  
  
  
  protected def handleEvents: Receive = {
    case msg @ Event(subscriptionId, _, _, _) =>
      log.debug("<-- {}", msg)
      eventHandlers.get(subscriptionId) match {
        case Some(handler) => handler(msg)
        case None => log.warn("!!! event handler not found for subscriptionId {}", subscriptionId)
      }
  }
}

object Subscriber {
  /**
    * A pending subscription
    * 
    * @param subscribe
    * @param handler
    * @param promise
    */
  class PendingSubscription(
    val subscribe: Subscribe, 
    val handler: EventHandler, 
    val promise: Promise[Subscription]
  )
}

/**
  * A subscription
  *
  * @param subscriber is the subscriber which has subscribed the topic
  * @param topic is the subscribed topic URI
  * @param subscribed is the message from replied back from the router
  */
class Subscription private[client] (subscriber: Subscriber, val topic: Uri, val subscribed: Subscribed) {
  /**
    * Unsubscribe this subscription
    *
    * @return a (future of) unsubscribed
    */
  def unsubscribe(): Future[Unsubscribed] = subscriber.unsubscribe(topic)
}

