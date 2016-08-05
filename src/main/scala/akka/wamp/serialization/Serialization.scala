package akka.wamp.serialization

import akka.wamp.messages._
import org.scalactic.Or

trait Serialization {
  
  type T
  
  def serialize(msg: Message): T
  
  def deserialize(t: T): Message Or DeserializationError
}


class DeserializationError(val message: String, val throwable: Throwable)