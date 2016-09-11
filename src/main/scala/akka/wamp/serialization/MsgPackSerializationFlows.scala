package akka.wamp.serialization

import akka.http.scaladsl.model.ws.{Message => WebSocketMessage}
import akka.stream.scaladsl.Flow
import akka.wamp.messages.{Message => WampMessage}


class MsgpackSerializationFlows extends SerializationFlows {
  
  override val serialize: Flow[WampMessage, WebSocketMessage, _] = ???
  
  override val deserialize: Flow[WebSocketMessage, WampMessage, _] = ???
}
