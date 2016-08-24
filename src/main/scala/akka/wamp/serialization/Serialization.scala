package akka.wamp.serialization

import akka.wamp.messages._

trait Serialization {
  
  type T
  
  def serialize(msg: Message): T
  
  @throws(classOf[DeserializeException])
  def deserialize(text: String)(implicit validator: Validator): Message
}


class DeserializeException(message: String, cause: Throwable) extends Exception(message, cause)
