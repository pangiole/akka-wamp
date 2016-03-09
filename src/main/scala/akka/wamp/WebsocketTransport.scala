package akka.wamp


import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage, Message => WebsocketMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}


/**
  * The default transport binding for WAMP is WebSocket.
  * 
  * The WAMP protocol MUST BE negotiated during the WebSocket opening 
  * handshake between Peers using the WebSocket subprotocol negotiation 
  * mechanism.
  *
  * @param peer the passive (listener) peer
  * @param m the actor materializer
  */
// TODO how about 5.3.2.  Transport and Session Lifetime
class WebsocketTransport private(peer: Peer)(implicit m: ActorMaterializer) extends Transport {

  val route = {
    path("transport") {
      handleWebSocketMessagesForProtocol(jsonFlowHandler, "wamp.2.json")
    }
  }

  
  private val jsonFlowHandler = Flow[WebsocketMessage]
    .mapConcat {
      case tm: TextMessage.Strict => {
        // val incoming = unmarshall(tm.text)
        // val outgoing = peer.process(incoming)
        // TextMessage(outgoing) :: Nil
        TextMessage(tm.text) :: Nil
      }
      case tm: TextMessage => {
        tm.textStream.runWith(Sink.ignore)
        Nil
      }
      case bm: BinaryMessage => {
        // ignore binary messages but drain content to avoid the stream being clogged
        bm.dataStream.runWith(Sink.ignore)
        Nil
      }  
    }
}

/**
  * 
  */
object WebsocketTransport {
  def apply(peer: Peer)(implicit m: ActorMaterializer) = new WebsocketTransport(peer)
}
