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
