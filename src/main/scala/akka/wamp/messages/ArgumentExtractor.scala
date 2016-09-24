package akka.wamp.messages

import scala.concurrent.Future

/**
  * A payload holder that can extract (parse) arguments
  */
trait ArgumentExtractor { this: PayloadContainer =>
  /**
    * Extract arguments parsing the payload with the
    * most appropriate parser
    *
    * @return the arguments as list of arbitrary types
    */
  def arguments(): Future[List[Any]] = {
    payload match {
      case Some(payload) => payload.arguments
      case None => Future.successful(List.empty)
    }
  }
  /**
    * Extract arguments parsing the payload with the
    * most appropriate parser
    *
    * @return the arguments as dictionary with values of arbitrary types
    */
  def argumentsKw(): Future[Map[String, Any]] = {
    payload match {
      case Some(payload) => payload.argumentsKw
      case None => Future.successful(Map.empty)
    }
  }
}