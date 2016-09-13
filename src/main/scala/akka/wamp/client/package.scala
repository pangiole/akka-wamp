package akka.wamp

import akka.wamp.client.Client
import akka.wamp.messages.{Event, Invocation}

/**
  * This package provides objects, classes and traits you can use to
  * write a WAMP [[Client]]
  */
package object client {

  /**
    * A type synonym for event handlers
    */
  type EventHandler = Event => Unit

  /**
    * A type synonym for invocation handlers
    */
  type InvocationHandler = Invocation => Unit
}
