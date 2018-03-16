package akka.wamp.client.japi


import akka.wamp.messages.{Event => EventDelegate}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext


class Event(delegate: EventDelegate)(implicit executionContent: ExecutionContext) extends DataConveyor(delegate) {

  val subscriptionId: Long = delegate.subscriptionId
  
  val publicationId: Long = delegate.publicationId
  
  val details = delegate.details.asJava
  
}
