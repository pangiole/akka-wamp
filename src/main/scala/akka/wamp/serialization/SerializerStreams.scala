package akka.wamp.serialization

import akka.http.scaladsl.model.ws.{Message => WebSocketMessage}
import akka.stream.scaladsl.Flow
import akka.wamp.Wamp.WampMessage

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


