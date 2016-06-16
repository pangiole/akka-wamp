package akka.wamp.router

import akka.actor._
import akka.http.scaladsl.model.ws.{TextMessage, Message => WebSocketMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream._
import akka.stream.scaladsl._
import akka.wamp.serialization.JsonSerialization
import akka.wamp.Wamp.{Message, SpecifiedMessage, Failure, Connected, ConnectionClosed}

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
class WebSocketHandler(router: ActorRef)(implicit system: ActorSystem, m: ActorMaterializer) {

  implicit val ec: ExecutionContext = system.dispatcher
  
  val route = {
    path("wamp") {
      handleWebSocketMessagesForProtocol(wampJsonHandler, "wamp.2.json")  
    }
  }

  
  // A stream source that will be materialized as an actor and
  // that will emit messages being serialized out to the websocket
  private val outgoingSource: Source[Message, ActorRef] = 
    Source.actorRef[Message](4, OverflowStrategies.Fail)
  
  
  var wampJsonHandler = Flow.fromGraph(
    GraphDSL.create(outgoingSource) {
      implicit builder => outgoingSource =>
        import GraphDSL.Implicits._

        val jsonSer = new JsonSerialization
        
        // As soon as a websocket connection is established with a client
        // the following materialized outlet will emit the Connect signal which:
        //
        //  - holds the streamActorSource reference, and
        //  - goes downstream to the transportSink via a merge junction
        
        val onConnect = builder.materializedValue.map(actorRef => Connected(actorRef))
        
        // NOTE: the actorRef carried by Transport.Open corresponds to the actorSource
        
        
        // The fromWebSocket flow
        //   - receives incoming websocket messages from the connected client, and
        //   - deserialize them to wamp messages going downstream to the transportSink
        val fromWebSocket = builder.add(Flow[WebSocketMessage]
          .collect[Message] {
            case TextMessage.Strict(text) => {
              val msg = jsonSer.deserialize(text)
              msg
            }
          }
          .log("incoming")
        )
        
        // Create 
        //  - a new outgoingActor instance passing the router actor reference, and 
        //  - a new stream transportSink which sends any signal to the outgoingActor
        val outgoingActor = system.actorOf(WebSocketTransport.props(router))
        val transportSink = Sink.actorRef[Message](outgoingActor, ConnectionClosed)

        // The merge junction sends all signals fromWebSocket downstream to transportSink
        val merge = builder.add(Merge[Message](2))

        // The toWebSocket flow
        //   - receives outgoing wamp messages from outgoingActor
        //      - serialize them to websocket messages for the connected client
        //   - receives exceptions from outgoingActor
        //      - close connection
        val toWebSocket = builder.add(Flow[Message]
          .log("outgoing")
          .collect {
            case msg: SpecifiedMessage =>
              val text = jsonSer.serialize(msg)
              TextMessage(text)
            case err: Failure => 
              // TODO how to complete this flow rather than throwing exception?
              throw new Exception(err.message)
          }
        )

        // Define stream topology
        /*|~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~|*/
        /*|                                                                                     |*/
        fromWebSocket ~> merge ~> transportSink  /*outgoingActor~>*/                          /*|*/
            onConnect ~> merge                                                                /*|*/
        /*|*/                                    /*outgoingActor~>*/  outgoingSource ~> toWebSocket
        /*|                                                                                     |*/
        /*|~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~|*/
        
        // Expose input/output ports
        FlowShape(fromWebSocket.in, toWebSocket.out)
    }
  )
}



