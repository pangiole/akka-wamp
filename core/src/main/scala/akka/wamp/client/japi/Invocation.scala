package akka.wamp.client.japi

import akka.wamp.messages.{Invocation => InvocationDelegate}

import scala.collection.JavaConversions.mapAsJavaMap
import scala.concurrent._


class Invocation(delegate: InvocationDelegate)(implicit executionContent: ExecutionContext) extends DataConveyor(delegate) {

  /**
    * Is the identifier of the registration
    */
  val registrationId: Long = delegate.registrationId

  /**
    * Are additional details
    */
  val details = mapAsJavaMap(delegate.details)
  
}
