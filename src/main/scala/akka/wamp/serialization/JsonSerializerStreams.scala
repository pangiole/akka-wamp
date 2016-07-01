package akka.wamp.serialization

import akka.NotUsed
import akka.http.scaladsl.model.ws.{TextMessage, Message => WebSocketMessage}
import akka.stream.scaladsl.Flow
import akka.wamp.Wamp._


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
