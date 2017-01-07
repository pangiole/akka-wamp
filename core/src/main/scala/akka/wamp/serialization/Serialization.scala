package akka.wamp.serialization

import akka.stream._
import akka.stream.scaladsl._
import akka.wamp.Validator
import akka.wamp.messages._

trait Serialization {
  
  type T

  def serialize(message: ProtocolMessage): Source[T, _]
  
  @throws(classOf[DeserializeException])
  def deserialize(source: Source[T, _])(implicit validator: Validator, mat: Materializer): ProtocolMessage
}


class DeserializeException(message: String, cause: Throwable) extends Exception(message, cause) {
  def this(message: String) = this(message, null)
}
