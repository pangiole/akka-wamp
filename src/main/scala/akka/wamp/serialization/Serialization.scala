package akka.wamp.serialization

import akka.stream.Materializer
import akka.wamp.Validator
import akka.wamp.messages._

trait Serialization {
  
  type T
  
  def serialize(message: Message): T
  
  @throws(classOf[DeserializeException])
  def deserialize(source: T)(implicit validator: Validator, materializer: Materializer): Message
}


class DeserializeException(message: String, cause: Throwable) extends Exception(message, cause) {
  def this(message: String) = this(message, null)
}
