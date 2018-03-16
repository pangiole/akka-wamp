package akka.wamp.client.japi

import akka.wamp.messages.{Invocation => InvocationDelegate}

import scala.collection.JavaConverters._
import scala.concurrent._


class Invocation(delegate: InvocationDelegate)(implicit executionContent: ExecutionContext) extends DataConveyor(delegate) {

  /**
    * Is the identifier of the registration
    */
  val registrationId: Long = delegate.registrationId

  /**
    * Are additional details
    */
  val details = delegate.details.asJava
  
}
