package akka.wamp.serialization

import akka.wamp.Wamp.WampMessage

trait Serialization[T] {
  
  def serialize(msg: WampMessage): T
  
  def deserialize(t: T): WampMessage
}

class SerializingException(message: String, cause: Throwable) extends Exception(message, cause) {
  def this(message: String) = this(message, null)
}
