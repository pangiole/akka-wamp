package akka.wamp.serialization


object Serializers {
  val streams = Map(
    "wamp.2.json" -> JsonSerializerStreams
    // TODO "wamp.2.msgpack" -> MsgPackSerializerStreams 
  )
}
