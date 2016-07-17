package akka.wamp.serialization

import akka.wamp.messages._

trait Serialization {
  
  type T
  
  def serialize(msg: Message): T
  
  @throws[SerializationException]
  def deserialize(t: T): Message
}


class SerializationException(message: String, cause: Throwable) extends Exception(message, cause) {
  def this(message: String) = this(message, null)
}
