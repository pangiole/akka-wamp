package akka.wamp


class Message(val code: Int) {
  
  def toJson(): String = "" // TODO JsonSerialization.serialize(this)
  
  def toMsgpack(): Array[Byte] = Array(0.toByte) // TODO MsgpackSerialization.serialize(this)
}



trait MessageBuilder {
  
  def build(): Message
}

