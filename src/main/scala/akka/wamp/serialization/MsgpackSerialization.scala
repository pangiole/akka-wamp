package akka.wamp.serialization

import akka.wamp.messages._

class MsgpackSerialization extends Serialization {
  
  type T = Array[Byte]

  def serialize(msg: Message): Array[Byte] = ???

  def deserialize(t: Array[Byte]): Message = ???
}
