package akka.wamp.serialization

import akka.wamp.messages._

class MsgpackSerialization extends Serialization {
  
  type T = Array[Byte]

  override def serialize(msg: Message): Array[Byte] = ???

  @throws(classOf[DeserializeException])
  override def deserialize(text: String)(implicit validator: Validator): Message = ???
}
