package akka.wamp.serialization

import akka.NotUsed
import akka.http.scaladsl.model.ws.{TextMessage, Message => WebSocketMessage}
import akka.stream.scaladsl.Flow
import akka.wamp.messages.{Message => WampMessage}

/**
  * Defines Akka Streams to serialize/deserialize messages
  */
trait SerializerStreams {

  /**
    * Serialize from WampMessage object to (textual or binary) WebSocketMessage
    */
  val serialize: Flow[WampMessage, WebSocketMessage, _]

  /**
  * Deserialize from (textual or binary) WebSocketMessage to WampMessage object
  */
  val deserialize: Flow[WebSocketMessage, WampMessage, _]
}


object JsonSerializerStreams extends SerializerStreams {

  val json = new JsonSerialization

  /**
    * Serialize from WampMessage object to textual WebSocketMessage
    */
  val serialize: Flow[WampMessage, WebSocketMessage, NotUsed] =
    Flow[WampMessage]
      //.log("toWebSocket")
      .map {
      case msg: WampMessage =>
        TextMessage.Strict(json.serialize(msg))
    }

  /**
    * Deserialize textual WebSocketMessage to WampMessage object
    */
  val deserialize: Flow[WebSocketMessage, WampMessage, NotUsed] =
    Flow[WebSocketMessage]
      .map {
        //TODO what happens when deserialize throws Exception?
        case TextMessage.Strict(text) =>
          json.deserialize(text)
      }
  //.log("fromWebSocket")
}


object Serializers {
  val streams = Map(
    "wamp.2.json" -> JsonSerializerStreams
    // TODO "wamp.2.msgpack" -> MsgPackSerializerStreams 
  )
}
