package akka.wamp.serialization

import akka.util.ByteString
import akka.wamp.Dict

import scala.concurrent.Future

/**
  * A payload
  */
sealed trait Payload {
  /**
    * @return arguments as list
    */
  def arguments(): Future[List[Any]]

  /**
    * @return arguments as dictionary
    */
  def argumentsKw(): Future[Dict]
}

object Payload {
  def apply(args: List[Any]) = new Payload {
    def arguments(): Future[List[Any]] = Future.successful(args)
    def argumentsKw(): Future[Dict] = Future.successful(args.zipWithIndex.map{case (a,i) => s"arg$i" -> a}.toMap)
  }
  def apply(args: Dict) = new Payload {
    def arguments(): Future[List[Any]] = Future.successful(args.keySet.toList)
    def argumentsKw(): Future[Dict] = Future.successful(args)
  }
}

abstract class AbstractPayload extends Payload {
  def arguments(): Future[List[Any]] = Future.failed(new PayloadException("Unknown serialization"))
  def argumentsKw(): Future[Dict] = Future.failed(new PayloadException("Unknown serialization"))
}


/**
  * A binary payload
  */
abstract class BinaryPayload extends AbstractPayload {
  /**
    * @return contents of this payload as a stream
    */
  def source(): ByteString
  override def toString: String = "BinaryPayload(...)"
}

/**
  * A textual payload
  */
abstract class TextPayload extends AbstractPayload {
  /**
    * @return contents of this payload as a stream
    */
  def source(): String
  override def toString: String = "TextPayload(...)"
}


object TextPayload {
  def apply(text: String) = new TextPayload {
    def source(): String = text
  } 
}

class PayloadException(message: String, cause: Throwable) extends Exception(message, cause) {
  def this(message: String) = this(message, null)
}
