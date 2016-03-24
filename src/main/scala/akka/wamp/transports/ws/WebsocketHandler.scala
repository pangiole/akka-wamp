package akka.wamp.transports.ws

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.ws.{Message => WebSocketMessage, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategies}
import akka.wamp.messages.{CloseTransport, OpenTransport}
import akka.wamp.serializations.JsonSerialization
import akka.wamp.{Message => WampMessage, Router, Transport}

import scala.concurrent.ExecutionContext


/**
  * The default transport binding for WAMP is WebSocket.
  * 
  * The WAMP protocol MUST BE negotiated during the WebSocket opening 
  * handshake between Peers using the WebSocket subprotocol negotiation 
  * mechanism.
  *
  * @param m the actor materializer
  */
// TODO how about 5.3.2.  Transport and Session Lifetime
class WebsocketHandler()(implicit system: ActorSystem, m: ActorMaterializer) extends Transport {

  implicit val ec: ExecutionContext = system.dispatcher
  
  val route = {
    path("wamp") {
        handleWebSocketMessagesForProtocol(jsonFlow, "wamp.2.json") 
      /* TODO ~ handleWebSocketMessagesForProtocol(msgpackFlow, "wamp.2.msgpack")*/ 
    }
  }

  private val jsonSer = new JsonSerialization
  /*
  private val jsonFlow = Flow.fromGraph(new JsonGraph(jsonSer))
    .map { msg =>
      router ! handle(msg)
      TextMessage(jsonSer.serialize(reply))
    }
  */

  
  var jsonFlow = Flow.fromGraph(
    GraphDSL.create(Source.actorRef[WampMessage](4, OverflowStrategies.Fail)) {
      implicit builder => routerSource =>
        import GraphDSL.Implicits._


        // As soon as a new WebSocket connection is established with a client
        // thisFlow will emit the OpenTransport message which:
        //
        //  - carries the actor reference of its materialization, and
        //  - goes to the downstream routerSink where it will be held for later usage
        
        val thisFlow = builder.materializedValue.map(ref => OpenTransport(ref))
        
        
        // The fromWebSocket flow
        // 
        //   - receives incoming WebSocket messages from the connected client, and
        //   - deserialize them to Wamp messages going to the downstream routerSink
        
        val fromWebSocket = builder.add(Flow[WebSocketMessage]
          .collect[WampMessage] {
            case TextMessage.Strict(text) => {
              jsonSer.deserialize(text).get
              // TODO drop failures
            }
            // TODO case tm: TextMessage => tm.textStream
            // TODO case bm: BinaryMessage => bm.dataStream  
          }
        )
        
        // Create a new routerActor instance 
        // and a sink which sends WAMP messages from the stream
        
        val routerActor = system.actorOf(Props[Router])
        val routerSink = Sink.actorRef[WampMessage](routerActor, CloseTransport)

        val merge = builder.add(Merge[WampMessage](2))
        
             thisFlow ~> merge
        fromWebSocket ~> merge ~> routerSink

        val toWebSocket = builder.add(Flow[WampMessage]
          .map[WebSocketMessage] {
            case msg: WampMessage =>
              TextMessage(jsonSer.serialize(msg))
          }
        )

        routerSource ~> toWebSocket
        
        
        // finally expose ports of this graph
        FlowShape(fromWebSocket.in, toWebSocket.out)
    }
  )
}

