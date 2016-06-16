package akka.wamp.serialization

import akka.wamp.Wamp.SpecifiedMessage

trait Serialization[T] {
  
  def serialize(msg: SpecifiedMessage): T
  
  def deserialize(t: T): SpecifiedMessage
}

class SerializingException(message: String, cause: Throwable) extends Exception(message, cause) {
  def this(message: String) = this(message, null)
}
