package akka.wamp.serialization

import akka.stream._
import akka.stream.scaladsl._
import akka.wamp.Validator
import akka.wamp.messages._

trait Serialization {
  
  type T

  def serialize(message: Message): Source[T, _]
  
  @throws(classOf[DeserializeException])
  def deserialize(source: Source[T, _])(implicit validator: Validator, mat: Materializer): Message
}


class DeserializeException(message: String, cause: Throwable) extends Exception(message, cause) {
  def this(message: String) = this(message, null)
}
