package akka.wamp.serialization

import akka.stream._
import akka.stream.scaladsl._
import akka.wamp.Validator
import akka.wamp.messages._

trait Serialization {
  
  type T

  // TODO def serialize(message: Message): Source[T, _]
  def serialize(message: Message)(implicit mat: Materializer): T
  
  @throws(classOf[DeserializeException])
  def deserialize(source: Source[T, _])(implicit validator: Validator, mat: Materializer): Message
}


class DeserializeException(message: String, cause: Throwable) extends Exception(message, cause) {
  def this(message: String) = this(message, null)
}
