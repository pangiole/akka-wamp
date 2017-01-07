package akka.wamp.serialization

import akka.http.scaladsl.model.ws.{Message => WebSocketMessage}
import akka.stream.scaladsl.Flow
import akka.wamp.messages.ProtocolMessage


class MsgPackSerializationFlows extends SerializationFlows {
  
  override val serialize: Flow[ProtocolMessage, WebSocketMessage, _] = ???
  
  override val deserialize: Flow[WebSocketMessage, ProtocolMessage, _] = ???
}
