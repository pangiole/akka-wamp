package akka.wamp.transports

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.{Message => WebSocketMessage, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategies}
import akka.wamp.{JsonSerialization, Message => WampMessage, Signal, Transport}
import akka.wamp.Router.ProtocolError
import scala.concurrent.ExecutionContext


/**
  * The default transport binding for WAMP is WebSocket.
  * 
  * The WAMP protocol MUST BE negotiated during the WebSocket opening 
  * handshake between Peers using the WebSocket subprotocol negotiation 
  * mechanism.
  *
  * @param router the router actor reference
  * @param system the actor system
  * @param m the actor materializer
  */
// TODO how about 5.3.2.  Transport and Session Lifetime
class HttpRequestHandler(router: ActorRef)(implicit system: ActorSystem, m: ActorMaterializer) {

  implicit val ec: ExecutionContext = system.dispatcher
  
  val route = {
    path("wamp") {
      handleWebSocketMessagesForProtocol(wampJsonHandler, "wamp.2.json") 
      /* TODO ~ handleWebSocketMessagesForProtocol(msgpackFlow, "wamp.2.msgpack")*/ 
    }
  }

  
  // A stream source that will be materialized as an actor and
  // that will emit signals being serialized out to the websocket
  private val streamActorSource = Source.actorRef[Signal](4, OverflowStrategies.Fail)
  
  
  var wampJsonHandler = Flow.fromGraph(
    GraphDSL.create(streamActorSource) {
      implicit builder => streamActorSource =>
        import GraphDSL.Implicits._

        val jsonSer = new JsonSerialization
        
        // As soon as a websocket connection is established with a client
        // the following materialized outlet will emit the Connect signal which:
        //
        //  - holds the streamActorSource reference, and
        //  - goes downstream to the transportSink via a merge junction
        
        val onConnect = builder.materializedValue.map(actorRef => Transport.Connect(actorRef))
        
        // NOTE: the actorRef carried by Transport.Open corresponds to the actorSource
        
        
        // The fromWebSocket flow
        //   - receives incoming websocket messages from the connected client, and
        //   - deserialize them to wamp messages going downstream to the transportSink
        val fromWebSocket = builder.add(
          Flow[WebSocketMessage].collect {
            case TextMessage.Strict(text) => {
              jsonSer.deserialize(text).get
            }
            case tm: TextMessage => {
              // TODO jsonSer.deserialize(tm.textStream); // convert to java.io.Reader ???
              ???
            }
            // TODO case bm: BinaryMessage => bm.dataStream  
          }
        )
        
        // Create 
        //  - a new transportActor instance passing the router actor reference, and 
        //  - a new stream transportSink which sends any signal to the transportActor
        val transportActor = system.actorOf(Transport.props(router))
        val transportSink = Sink.actorRef[Signal](transportActor, Transport.Disconnect)

        // The merge junction sends all signals fromWebSocket downstream to transportSink
        val merge = builder.add(Merge[Signal](2))

        // The toWebSocket flow
        //   - receives outgoing wamp messages from transportActor
        //      - serialize them to websocket messages for the connected client
        //   - receives exceptions from transportActor
        //      - close connection
        val toWebSocket = builder.add(
          Flow[Signal].collect {
            case msg: WampMessage =>
              TextMessage(jsonSer.serialize(msg))
            case err: ProtocolError =>
              throw new Exception(err.message)
          }
        )

        // Define stream topology
        /*|~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~|*/
        /*|                                                                                   |*/
        fromWebSocket ~> merge ~> transportSink /*transportActor~>*/                        /*|*/
            onConnect ~> merge                                                              /*|*/
        /*|*/                                   /*transportActor~>*/ streamActorSource ~> toWebSocket
        /*|                                                                                   |*/
        /*|~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~|*/
        
        // Expose input/output ports
        FlowShape(fromWebSocket.in, toWebSocket.out)
    }
  )
}



