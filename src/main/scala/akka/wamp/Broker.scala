package akka.wamp

import akka.actor.ActorRef
import akka.wamp.Router._
import akka.wamp.Messages._

/**
  * A Broker routes events incoming from [[Publisher]]s to [[Subscriber]]s 
  * that are subscribed to respective [[Topic]]s
  */
trait Broker extends Role { this: Router =>

  /**
    * Map of subscriptions
    */
  var subscriptions = Map.empty[Long, Subscription]
  
  
  def handleSubscriptions: Receive = {
    
    case Subscribe(requestId, options, topic) =>
      ifSessionOpen { session =>
        subscriptions.values.toList.filter(_.topic == topic) match {
          case Nil => {
            /**
              * It's the first time the client subscribes to the topic and
              * the subscription ID chosen by the broker may be assigned 
              * to the topic, or the combination of the topic and some or all 
              * options, such as the topic pattern matching method to be used. 
              */
            val subscriptionId = nextId(subscriptions, _ + 1)
            subscriptions += (subscriptionId -> new Subscription(subscriptionId, Set(session.client), topic))
            session.client ! Subscribed(requestId, subscriptionId)
          }
          case subscription :: Nil => {
            if (!subscription.subscribers.contains(session.client)) {
              /**
                * In case of receiving a SUBSCRIBE message from a client to the 
                * topic already subscribed by others, broker should update the 
                * subscribers set of the existing subscription and answer with 
                * SUBSCRIBED message, containing the existing subscription ID. 
                */
              subscriptions += (subscription.id -> subscription.copy(subscribers = subscription.subscribers + session.client))
            }
            else {
              /**
                * In case of receiving a SUBSCRIBE message from the same subscriber 
                * to already subscribed topic, broker should answer with 
                * SUBSCRIBED message, containing the existing subscription ID.
                */
            }
            session.client ! Subscribed(requestId, subscription.id)
          }
          case _ => throw new IllegalStateException()
        }
      }
      
    case Unsubscribe(requestId, subscriptionId) =>
      ifSessionOpen { session =>
        subscriptions.get(subscriptionId) match {
          case Some(subscription) =>
            unsubscribe(session.client, subscription)
            session.client ! Unsubscribed(requestId)
          case None => 
            session.client ! Error(UNSUBSCRIBE, requestId, DictBuilder().build(), "wamp.error.no_such_subscription")
        }
      }
  }
  
  
  def ifSessionOpen(fn: (Session) => Unit): Unit = {
    switchOn(sender())(
      whenSessionOpen = { session => 
        fn(session)
      },
      otherwise = { _ =>
        sender() ! ProtocolError("Session was not open yet.")
      }
    )
  }

  def findSubscriptionBy(clientRef: ActorRef, topic: Uri)(whenFound: (Subscription) => Unit, otherwise: (ActorRef) => Unit): Unit = {
    subscriptions.values.find(s => s.subscribers.contains(clientRef) && s.topic == topic) match {
      case Some(subscription) => whenFound(subscription)
      case None => otherwise(clientRef)
    }
  }
  
  def unsubscribe(client: ActorRef, subscription: Subscription) = {
    if (subscription.subscribers.contains(client)) {
      if (subscription.subscribers.size == 1) {
        subscriptions -= subscription.id
      } else {
        subscriptions += (subscription.id -> subscription.copy(subscribers = subscription.subscribers - client))
      }
    }
  }
  
}

