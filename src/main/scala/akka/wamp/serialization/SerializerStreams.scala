package akka.wamp.serialization

import akka.NotUsed
import akka.actor.SupervisorStrategy.Directive
import akka.http.scaladsl.model.ws.{TextMessage, Message => WebSocketMessage}
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.Supervision
import akka.stream.Supervision.{Decider, Resume}
import akka.stream.scaladsl.Flow
import akka.wamp.messages.{Message => WampMessage}
import org.scalactic.{Bad, Good}
import org.slf4j.LoggerFactory

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
  val log = LoggerFactory.getLogger(classOf[SerializerStreams])
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
        case TextMessage.Strict(text) => json.deserialize(text)
        case m => ??? // TODO what to do for Streamed(_)
      }
      .withAttributes(supervisionStrategy {
        case ex: DeserializeException =>
          log.warn(ex.getMessage)
          Supervision.Resume
      })
}


object Serializers {
  val streams = Map(
    "wamp.2.json" -> JsonSerializerStreams
  )
}
