package akka.wamp.client.japi

import akka.wamp.messages.{Result => ResultDelegate}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

/**
  * Result representative for Java API
  *
  * {{{
  *   // CompletionStage[Result] result = ...
  *
  *   result.whenComplete((res, ex) -> {
  *     if (res != null) {
  *       // access conveyed arguments
  *     }
  *     else
  *       system.log().error(ex.getMessage(), ex);
  *   });
  * }}}
  *
  * @param delegate is the result delegate 
  */
class Result(delegate: ResultDelegate)(implicit executionContent: ExecutionContext) extends DataConveyor(delegate) {

  /**
    * Is the identifier as in the original call request
    */
  val requestId = delegate.requestId

  /**
    * Is the dictionary of additional details
    */
  val details = delegate.details.asJava
}
