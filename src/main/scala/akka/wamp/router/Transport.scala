package akka.wamp.router

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message => WebSocketMessage}
import akka.http.scaladsl.server.Directives.{get, handleWebSocketMessagesForProtocol, path}
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Sink, Source}
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategies}
import akka.wamp.Wamp._
import akka.wamp._
import akka.wamp.messages.{Message => WampMessage, _}
import akka.wamp.serialization._


/**
  * A Transport connects two [[Peer]]s and provides a channel over which 
  * [[Message]]s for a [[Session]] can flow in both directions.
  *
  * @param router is the first peer that will be connected by this transport
  */
class Transport(router: ActorRef)(implicit mat: ActorMaterializer)
extends akka.wamp.Transport 
with Actor with ActorLogging 
{
  /**
    * It is the second peer that will be connected by this transport
    */
  var client: ActorRef = _

  /**
    * It is the WebSocket flow handler for WAMP
    */
  val websocketHandler: Flow[WebSocketMessage, WebSocketMessage, ActorRef] = {
    val serializer = Serializers.streams("wamp.2.json")

    // A stream source that will be materialized as an actor and
    // that will emit WAMP messages being serialized out to the websocket
    val transportSource: Source[WampMessage, ActorRef] =
      Source.actorRef[WampMessage](bufferSize = 4, OverflowStrategies.Fail)
    
    Flow.fromGraph(GraphDSL.create(transportSource) {
      implicit builder => transportSource =>
        import GraphDSL.Implicits._

        // As soon as a new WebSocket connection is established with a client
        // then the following materialized outlet:
        //   - will emit the Connected signal carrying the clientActor reference, and
        //   - will go downstream to the transportSink via a merge junction
        val onConnect = builder.materializedValue.map(clientActorRef => 
          Connected(clientActorRef)
        )

        // The fromWebSocket flow
        //   - receives incoming WebSocketMessages from the connected client, and
        //   - deserialize them to WampMessages going downstream to the transportSink
        val fromWebSocket = builder.add(serializer.deserialize)

        // Create a new transportSink which delivers any message to this transportActor (self)
        val transportSink = Sink.actorRef[AbstractMessage](self, onCompleteMessage = Disconnected)

        // The merge junction forwards all messages fromWebSocket downstream to the transportSink
        val merge = builder.add(Merge[AbstractMessage](2))

        // The toWebSocket flow
        //   - receives outgoing WampMessages from the transportSource
        //   - serialize them to WebSocketMessges to the connected client
        val toWebSocket = builder.add(serializer.serialize)

        // Define stream topology
        /*|~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~|*/
        /*|                                                                                     |*/
        onConnect     ~> merge                                                                /*|*/
        fromWebSocket ~> merge ~> transportSink /* transportActor ~> */                       /*|*/
        /*|*/                                   /* transportActor ~> */ transportSource ~> toWebSocket
        /*|                                                                                     |*/
        /*|~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~|*/

        // Expose input/output ports
        FlowShape(fromWebSocket.in, toWebSocket.out)
    })
  }
  
  val httpHandler: Route = {
    get {
      path("ws") {
        handleWebSocketMessagesForProtocol(websocketHandler, "wamp.2.json")
        // TODO ~ handleWebSocketMessagesForProtocol(flow, "wamp.2.msgpack")
      }
    }
  }
  
  override def preStart(): Unit = {
    log.info("[{}] Starting", self.path.name)
  }

  override def postStop(): Unit = {
    log.debug("[{}] Stopped", self.path.name)
  }

  
  def receive: Receive = {
    case conn: Http.IncomingConnection =>
      log.debug("[{}] Connected to router [{}]", self.path.name, router.path.name)
      conn.handleWith(httpHandler)
      
    case conn: Wamp.Connected =>
      client = conn.ref
      log.debug("[{}] Connected to client [{}]", self.path.name, client.path.name)

    case msg: WampMessage if (sender() == router) =>
      log.debug("[{}] ~> {}", self.path.name, msg)
      client ! msg
      
    case msg: WampMessage /* couldAssert (sender() == client) */ =>
      log.debug("[{}] <~ {}", self.path.name, msg)
      router ! msg
      
    case Wamp.Disconnected =>
      log.debug("[{}] Disconnected from client [{}]", self.path.name, client.path.name)
      router ! Wamp.Disconnect
      client ! PoisonPill
      context.stop(self)

    case Failure(ex) =>
      log.warning("[{}] Failure due to {}: {}", self.path.name, ex.getClass.getName, ex.getMessage)
      client ! PoisonPill
      context.stop(self)
  }
  
  
}


object Transport {
  /**
    * Create a Props for an actor of this type
    * 
    * @return the props
    */
  def props(router: ActorRef)(implicit mat: ActorMaterializer) = Props(new Transport(router)(mat))
}
