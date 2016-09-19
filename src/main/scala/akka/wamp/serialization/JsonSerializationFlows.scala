package akka.wamp.serialization

import akka.NotUsed
import akka.http.scaladsl.model.{ws => websocket}
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.{Materializer, Supervision}
import akka.stream.scaladsl.Flow
import akka.wamp.Validator
import akka.wamp.{messages => wamp}
import org.slf4j.LoggerFactory

/**
  * The JSON serialization flows
  * 
  * @param validateStrictUri is the boolean switch (default is false) to validate against strict URIs rather than loose URIs
  * @param disconnectOffendingPeers is the boolean switch to disconnect those clients that send invalid messages
  * @param materializer is the Akka Stream materializer
  */
class JsonSerializationFlows(validateStrictUri: Boolean, disconnectOffendingPeers: Boolean)(implicit materializer: Materializer) 
  extends SerializationFlows 
{
  val log = LoggerFactory.getLogger(classOf[SerializationFlows])
  
  val json = new JsonSerialization()

  /** The WAMP types validator */
  implicit val validator = new Validator(validateStrictUri)


  /**
    * Serialize from wamp.Message object to textual websocket.Message
    */
  val serialize: Flow[wamp.Message, websocket.Message, NotUsed] =
    Flow[wamp.Message].
      map {
        case message: wamp.Message =>
          TextMessage(json.serialize(message))
      }



  /**
    * Deserialize textual websocket.Message to wamp.Message object
    */
  val deserialize: Flow[websocket.Message, wamp.Message, NotUsed] =
    Flow[websocket.Message]
      .map {
        case TextMessage.Strict(text) =>
          json.deserialize(text)

        case TextMessage.Streamed(source) =>
          throw new DeserializeException("Streaming not supported yet.")

        case m: BinaryMessage =>
          throw new DeserializeException("Cannot deserialize binary message as JSON message was expected instead!")
      }
      .withAttributes(supervisionStrategy {
        case ex: DeserializeException =>
          if (!disconnectOffendingPeers) {
            log.warn("Resume from DeserializeException: {}", ex.getMessage)
            Supervision.Resume
          }
          else {
            Supervision.Stop 
          }
      })
}