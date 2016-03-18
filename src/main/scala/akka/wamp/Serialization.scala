package akka.wamp

import scala.util.Try

/**
  * TODO A Serialization ...
  * 
  * @tparam T
  */
trait Serialization[T] {
  
  def serialize(m: Message): T
  
  def deserialize(t: T): Try[Message]
}
