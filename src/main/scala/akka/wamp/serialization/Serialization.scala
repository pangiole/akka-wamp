package akka.wamp.serialization

import akka.wamp.messages._
import org.scalactic.Or

trait Serialization {
  
  type T
  
  def serialize(msg: Message): T
  
  @throws(classOf[DeserializeException])
  def deserialize(t: T): Message
}


class DeserializeException(message: String, cause: Throwable) extends Exception(message, cause)
