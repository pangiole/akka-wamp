package akka.wamp

import akka.actor.ActorRef
import akka.wamp.Router._
import akka.wamp.Messages._

/**
  * A Broker routes events incoming from [[Publisher]]s to [[Subscriber]]s 
  * that are subscribed to respective [[Topic]]s
  */
trait Broker extends Role {
  this: Router =>

  /**
    * Map of subscriptions. Each entry is for one topic only 
    * and it can have one or many subscribers
    */
  var subscriptions = Map.empty[Long, Subscription]


  /**
    * Map of publications
    */
  var publications = Map.empty[Long, Publication]


  val emptyDict = DictBuilder().build()

  /**
    * Handle PUBLISH and EVENT messages
    */
  def handlePublications: Receive = {
    case Publish(requestId, options, topic, arguments, argumentsKw) =>
      ifSessionOpen { session =>
        val publisher = session.client
        if (session.clientRoles.contains("publisher")) {
          /**
            * By default, publications are unacknowledged, and the Broker will
            * not respond, whether the publication was successful indeed or not.
            * This behavior can be changed with the option
            *
            * "PUBLISH.Options.acknowledge|bool"
            */
          val ack = options.get("acknowledge") == Some(true)
          subscriptions.values.toList.filter(_.topic == topic) match {
            case Nil =>
              /**
                * Actually, no subscribers has subscribed to the given topic.
                * When the request for publication cannot be fulfilled by the Broker,
                * and "PUBLISH.Options.acknowledge == true", the Broker sends back an
                * "ERROR" message to the Publisher
                */
              if (ack) publisher ! Error(PUBLISH, requestId, emptyDict, "wamp.error.no_such_topic")
              ()
            case subscription :: Nil =>

              /**
                * When a publication is successful and a Broker dispatches the event,
                * it determines a list of receivers for the event based on subscribers
                * for the topic published to and, possibly, other information in the event.
                *
                * Note that the publisher of an event will never receive the published 
                * event even if the publisher is also a subscriber of the topic published to.
                */
              val publicationId = nextId(publications, _ + 1)
              subscription.subscribers.filter(_ != publisher).foreach { subscriber =>
                publications += (publicationId -> new Publication(publicationId))
                subscriber ! Event(subscription.id, publicationId, emptyDict, arguments, argumentsKw)
              }
              if (ack) publisher ! Published(requestId, publicationId)
            case _ => throw new IllegalStateException()
          }
        }
        else {
          publisher ! Error(PUBLISH, requestId, emptyDict, "akka.wamp.error.no_publisher_role")
        }
      }
  }

  /**
    * Handle SUBSCRIBE and UNSUBSCRIBE messages
    */
  def handleSubscriptions: Receive = {

    case Subscribe(requestId, options, topic) =>
      ifSessionOpen { session =>
        val subscriber = session.client
        if (session.clientRoles.contains("subscriber")) {
          subscriptions.values.toList.filter(_.topic == topic) match {
            case Nil => {
              /**
                * No subscribers has subscribed to the given topic yet
                */
              val subscriptionId = nextId(subscriptions, _ + 1)
              subscriptions += (subscriptionId -> new Subscription(subscriptionId, Set(subscriber), topic))
              subscriber ! Subscribed(requestId, subscriptionId)
            }
            case subscription :: Nil => {
              if (!subscription.subscribers.contains(subscriber)) {
                /**
                  * In case of receiving a SUBSCRIBE message from a client to the 
                  * topic already subscribed by others, broker should update the 
                  * subscribers set of the existing subscription and answer with 
                  * SUBSCRIBED message, containing the existing subscription ID. 
                  */
                subscriptions += (subscription.id -> subscription.copy(subscribers = subscription.subscribers + subscriber))
              }
              else {
                /**
                  * In case of receiving a SUBSCRIBE message from the same subscriber 
                  * to already subscribed topic, broker should answer with 
                  * SUBSCRIBED message, containing the existing subscription ID.
                  */
              }
              subscriber ! Subscribed(requestId, subscription.id)
            }
            case _ => throw new IllegalStateException()
          }
        }
        else {
          subscriber ! Error(SUBSCRIBE, requestId, emptyDict, "akka.wamp.error.no_subscriber_role")
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
        sender() ! Failure("Session was not open yet.")
      }
    )
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

