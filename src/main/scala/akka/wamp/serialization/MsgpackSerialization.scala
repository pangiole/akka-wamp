package akka.wamp.serialization

import akka.wamp.messages._
import org.scalactic.Or

class MsgpackSerialization extends Serialization {
  
  type T = Array[Byte]

  override def serialize(msg: Message): Array[Byte] = ???

  @throws(classOf[DeserializeException])
  override def deserialize(t: Array[Byte]): Message = ???
}
