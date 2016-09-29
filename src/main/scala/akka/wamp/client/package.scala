package akka.wamp

import akka.Done
import akka.wamp.messages._
import akka.wamp.serialization.Payload

import scala.collection.mutable
import scala.concurrent._

/**
  * This package provides objects, classes and traits you can use to
  * write a WAMP Client.
  * 
  *
  */
package object client {

  /**
    * Pending SUBSCRIBE messages are those waiting for 
    * a reply from the broker (SUBSCRIBED or ERROR)
    */
  type PendingSubscriptions = mutable.Map[/*Request*/Id, PendingSubscription]
 
  /**
    * Subscriptions are those confirmed with SUBSCRIBED
    */
  type Subscriptions = mutable.Map[/*Subscription*/Id, Subscription]

  /**
    * Pending UNSUBSCRIBE messages are those waiting for 
    * a reply from the broker (UNSUBSCRIBED or ERROR)
    */
  type PendingUnsubscribes = mutable.Map[/*Request*/Id, (Unsubscribe, Promise[Unsubscribed])]

  /**
    * Pending PUBLISH messages are those sent with ACK 
    * and waiting for a reply from the broker (PUBLISHED or ERROR)
    */
  type PendingPublications = mutable.Map[Id, Promise[Either[Done, Publication]]]

  
  /**
    * Type synonym for an event handler
    */
  type EventHandler = Event => Unit
  
  /**
    * Type synonym for subscribed event handlers
    */
  type EventHandlers = mutable.Map[/*Subscription*/Id, EventHandler]

  

  
  // ~~~~~~~~~~~~~~~~~~~~~~~~~~

  /**
    * Type synonym for an invocation handler
    */
  type InvocationHandler = Invocation => Future[Payload]
 
}
