package akka.wamp.router

import akka.actor._
import akka.wamp._
import akka.wamp.messages._
import scala.collection.mutable

/**
  * The broker routes events incoming from clients with publisher to 
  * clients with subscriber that are subscribed to respective topics
  */
trait Broker { this: Router =>

  /** Map of subscriptions. Each entry is for one topic only and it can have one or many subscribers */
  private[router] val subscriptions = mutable.Map.empty[Id, Subscription]

  /** Set of publication identifiers */
  private[router] val publications = mutable.Set.empty[Id]


  /** Handle publications lifecycle messages such as: PUBLISH */
  private[router] def handlePublications: Receive = {
    case msg@Publish(requestId, options, topic, payload) =>
      withSession(msg, sender()) { session =>
        /**
          * By default, publications are unacknowledged, and the Broker will
          * not respond, whether the publication was successful indeed or not.
          * This behavior can be changed with the option
          *
          * "PUBLISH.Options.acknowledge|bool"
          */
        val ack = options.get("acknowledge") == Some(true)
        val publicationId = idGenerators('global).nextId(excludes = publications.toSet)
        subscriptions.values.toList.filter(sub => (sub.topic == topic && sub.realm == session.realm)) match {
          case Nil =>
            /**
              * Actually, no subscribers has subscribed to the given topic.
              */
            if (ack) {
              session.peer ! Published(requestId, publicationId)
            }
          case subscription :: Nil =>
            /**
              * When a publication is successful and a Broker dispatches the event,
              * it determines a list of receivers for the event based on subscribers
              * for the topic published to and, possibly, other information in the event.
              *
              * Note that the publisher of an event will never receive the published 
              * event even if the publisher is also a subscriber of the topic published to.
              */
            subscription.subscribers.filter(_ != session.peer).foreach { subscriber =>
              publications += publicationId
              subscriber ! Event(subscription.id, publicationId, Dict(), payload)
            }
            if (ack) {
              session.peer ! Published(requestId, publicationId)
            }

          case _ =>
            throw new IllegalStateException()
        }
      }
  }

  /**
    * Handle subscriptions lifecycle messages such as: 
    * SUBSCRIBE and UNSUBSCRIBE
    */
  private[router] def handleSubscriptions: Receive = {
    case message@Subscribe(requestId, options, topic) =>
      withSession(message, sender()) { session =>
        subscriptions.values.toList.filter(sub => (sub.topic == topic && sub.realm == session.realm)) match {
          case Nil => {
            /**
              * No subscribers have subscribed to the given topic yet.
              */
            val subscriptionId = idGenerators('router).nextId(excludes = subscriptions.toMap.keySet)
            subscriptions += (subscriptionId -> new Subscription(subscriptionId, Set(session.peer), topic, session.realm))
            session.peer ! Subscribed(requestId, subscriptionId)
          }
          case subscription :: Nil => {
            if (!subscription.subscribers.contains(session.peer)) {
              /**
                * In case of receiving a SUBSCRIBE message from a client2 to the 
                * topic already subscribed by some other client1, broker should 
                * update the subscribers set of the existing subscription and 
                * answer SUBSCRIBED the existing subscription ID. 
                */
              subscriptions += (subscription.id -> subscription.copy(subscribers = subscription.subscribers + session.peer))
            }
            else {
              /**
                * In case of receiving a SUBSCRIBE message from the same subscriber 
                * to already subscribed topic, broker should answer with 
                * SUBSCRIBED message, containing the existing subscription ID.
                */
            }
            session.peer ! Subscribed(requestId, subscription.id)
          }
          case _ => {
            log.warning("[{}] !!! IllegalStateException: more than one subscription for topic {} found.", self.path.name, topic)
          }
        }
      }

      
    case message@Unsubscribe(requestId, subscriptionId) =>
      withSession(message, sender()) { session =>
        subscriptions.get(subscriptionId) match {
          case Some(subscription) =>
            if (unsubscribe(subscriber = session.peer, subscription)) {
              session.peer ! Unsubscribed(requestId)
            }
            else {
              // though the given subscriptionId does exist
              // it turned out to be for some other client
              session.peer ! Error(Unsubscribe.tpe, requestId, error = "wamp.error.no_such_subscription")
            }
          case None =>
            // subscriptionId does not exist
            session.peer ! Error(Unsubscribe.tpe, requestId, error = "wamp.error.no_such_subscription")
        }
      }
  }


  /**
    * Remove the given subscriber from the given subscription. If no subscriber are
    * left on the subscription then the given subscription will be removed as well.
    *
    * @param subscriber   is the client subscriber actor reference
    * @param subscription is the subscription the subscriber has to be removed from
    * @return ``true`` if given subscriber was really subscribed, ``false`` otherwise                     
    */
  private[router] def unsubscribe(subscriber: ActorRef, subscription: Subscription): Boolean = {
    if (subscription.subscribers.contains(subscriber)) {
      if (subscription.subscribers.size == 1) {
        subscriptions -= subscription.id
      } else {
        subscriptions += (subscription.id -> subscription.copy(subscribers = subscription.subscribers - subscriber))
      }
      true
    }
    else false
  }
}

