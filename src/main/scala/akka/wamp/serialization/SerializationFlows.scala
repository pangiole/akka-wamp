package akka.wamp.serialization

import akka.NotUsed
import akka.http.scaladsl.model.ws.{TextMessage, Message => WebSocketMessage}
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.Supervision
import akka.stream.scaladsl.Flow
import akka.wamp.messages.{Validator, Message => WampMessage}
import org.slf4j.LoggerFactory

/**
  * Defines Akka Stream flows meant to serialize/deserialize messages 
  * to/from textual(binary)/object representation
  */
trait SerializationFlows {
  
  /**
    * Serialize from WampMessage object to (textual or binary) WebSocketMessage
    */
  val serialize: Flow[WampMessage, WebSocketMessage, _]

  /**
  * Deserialize from (textual or binary) WebSocketMessage to WampMessage object
  */
  val deserialize: Flow[WebSocketMessage, WampMessage, _]
}


class JsonSerializationFlows(val validator: Validator) extends SerializationFlows {
  val log = LoggerFactory.getLogger(classOf[SerializationFlows])
  val json = new JsonSerialization

  /**
    * Serialize from WampMessage object to textual WebSocketMessage
    */
  val serialize: Flow[WampMessage, WebSocketMessage, NotUsed] =
    Flow[WampMessage]
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
        case TextMessage.Strict(text) => json.deserialize(text)(validator)
        case m => ??? // TODO what to do for Streamed(_)
      }
      .withAttributes(supervisionStrategy {
        case ex: DeserializeException =>
          log.warn("DeserializeException: {}", ex.getMessage)
          Supervision.Resume
      })
}
