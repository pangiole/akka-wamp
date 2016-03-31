package akka.wamp

import akka.wamp.Router.ProtocolError
import akka.wamp.messages._
import akka.actor.{ActorRef, Actor, ActorLogging}

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
    case Subscribe(request, options, topic) => {
      findSessionBy(sender())(
        whenFound = (session) => {
          findSubscriptionBy(session.clientRef, topic)(
            whenFound = (subscription) => {
              /**
                * In case of receiving a SUBSCRIBE message from the same subscriber 
                * and to already subscribed topic, broker should answer with 
                * SUBSCRIBED message, containing the existing subscription ID.
                */
              session.clientRef ! Subscribed(request, subscription.id)
            },
            otherwise = (clientRef) => {
              subscribe(clientRef, request, options, topic)
            }
          )
        },
        otherwise = (clientRef) => {
          clientRef ! ProtocolError("Session was not open yet.")
        }
      )
    }
  }

  def findSubscriptionBy(clientRef: ActorRef, topic: Uri)(whenFound: (Subscription) => Unit, otherwise: (ActorRef) => Unit): Unit = {
    subscriptions.values.find(s => s.clientRefs.contains(clientRef) && s.topic == topic) match {
      case Some(subscription) => whenFound(subscription)
      case None => otherwise(clientRef)
    }
  }
  
  private def subscribe(clientRef: ActorRef, request: Id, options: Dict, topic: Uri) = {
    /**
      * The subscription ID chosen by the broker need not be unique 
      * to the subscription of a single subscriber, but may be assigned 
      * to the topic, or the combination of the topic and some or all 
      * options, such as the topic pattern matching method to be used. 
      */
    val id = generateSubscriptionId(subscriptions, topic.hashCode)

    /**
      * Subscription ID could have been already added
      */
    val clientRefs = subscriptions.get(id) match {
      case Some(subscription) => (subscription.clientRefs + clientRef)
      case None => Set(clientRef)
    }
    subscriptions += (id -> new Subscription(id, clientRefs, topic))
    clientRef ! Subscribed(request, id)
  }
}

