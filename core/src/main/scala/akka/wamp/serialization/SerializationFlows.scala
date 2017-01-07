package akka.wamp.serialization

import akka.http.scaladsl.model.ws.{Message => WebSocketMessage}
import akka.stream.scaladsl.Flow
import akka.wamp.messages.ProtocolMessage

/**
  * Defines Akka Stream flows meant to serialize/deserialize messages 
  * to/from textual(binary)/object representation
  */
trait SerializationFlows {
  
  /**
    * Serialize from WampMessage object to (textual or binary) WebSocketMessage
    */
  val serialize: Flow[ProtocolMessage, WebSocketMessage, _]

  /**
  * Deserialize from (textual or binary) WebSocketMessage to WampMessage object
  */
  val deserialize: Flow[WebSocketMessage, ProtocolMessage, _]
}



