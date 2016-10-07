package akka.wamp.router

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status => stream}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message => WebSocketMessage}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.{Route, Directives => dsl}
import akka.stream.{ActorMaterializer, FlowShape, Materializer, OverflowStrategies}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Sink, Source}
import akka.wamp.messages.Message
import akka.wamp.{Wamp, _}
import akka.wamp.serialization.{JsonSerializationFlows, SerializationFlows}
import com.typesafe.config.Config


/**
  * This connection connects two peers and provides a WebSocket channel
  * over which JSON messages for a session can flow in both directions.
  *
  * @param router is the first peer to connect
  * @param path
  * @param validateStrictUris 
  * @param disconnectOffendingPeers
  */
private 
class ConnectionHandler(router: ActorRef, path: String, validateStrictUris: Boolean, disconnectOffendingPeers: Boolean) 
  extends Actor 
    with ActorLogging 
{
  implicit val mat = ActorMaterializer()
  // TODO close the materializer at some point

  
  
  // TODO https://github.com/angiolep/akka-wamp/issues/12
  val serializationFlows = new JsonSerializationFlows(validateStrictUris, disconnectOffendingPeers)
  
  /** The second peer to connect */
  var peer: ActorRef = _
  
  val websocketHandler: Flow[WebSocketMessage, WebSocketMessage, ActorRef] = {

    // A stream source that will be materialized as an actor and
    // that will emit WAMP messages being serialized out to the websocket
    val transportSource: Source[Message, ActorRef] =
      Source.
        actorRef[Message](bufferSize = 4, OverflowStrategies.Fail)

    // Create a new transportSink which delivers any message to this transportActor (self)
    val transportSink: Sink[Wamp.ManagedMessage, NotUsed] =
      Sink.
        actorRef[Wamp.ManagedMessage](self, onCompleteMessage = Wamp.Disconnected)

    Flow.fromGraph(GraphDSL.create(transportSource) {
      implicit builder => transportSource =>
        import GraphDSL.Implicits._

        // As soon as a new WebSocket connection is established with a client
        // then the following materialized outlet:
        //   - will emit the Wamp.Connected signal carrying the clientActor reference, and
        //   - will go downstream to the transportSink via a merge junction
        val onConnect = builder.materializedValue.map(client => Wamp.Connected(client))

        // The fromWebSocket flow
        //   - receives incoming WebSocketMessages from the connected client, and
        //   - deserialize them to WampMessages going downstream to the transportSink
        val fromWebSocket = builder.add(serializationFlows.deserialize)

        // The merge junction forwards all messages fromWebSocket downstream to the transportSink
        val merge = builder.add(Merge[Wamp.ManagedMessage](2))

        // The toWebSocket flow
        //   - receives outgoing WampMessages from the transportSource
        //   - serialize them to WebSocketMessges to the connected client
        val toWebSocket = builder.add(serializationFlows.serialize)

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

  
  val httpRoute: Route = {
    dsl.get {
      dsl.path(path) {
        dsl.handleWebSocketMessagesForProtocol(websocketHandler, "wamp.2.json")
      }
    }
  }

  val reactToConnectionFailure: Flow[HttpRequest, HttpRequest, _] = {
    Flow[HttpRequest]
      .recover[HttpRequest] {
        // TODO It never gets called! It looks like an Akka issue. Do telnet close connection to replicate it.
        case ex => throw ex
      }
  }

  val httpFlow: Flow[HttpRequest, HttpResponse, NotUsed] = {
    Flow[HttpRequest].
      via(reactToConnectionFailure).
      via(httpRoute)
  }
  
  
  
  override def preStart(): Unit = {
    log.debug("[{}]     Starting", self.path.name)
  }

  override def postStop(): Unit = {
    log.debug("[{}]     Stopped", self.path.name)
  }
  
  def receive: Receive = {

    case conn: Http.IncomingConnection =>
      log.debug("[{}]     Http.Incoming accepted on {}", self.path.name, conn.localAddress)
      conn.handleWith(httpFlow)
      
    case signal @ Wamp.Connected(p) =>
      peer = p
      log.debug("[{}]     Wamp.Connected [{}]", self.path.name, peer.path.name)
      router ! signal

    case msg: Message if (sender() == router) =>
      log.debug("[{}] --> {}", self.path.name, msg)
      peer ! msg
      
    case msg: Message =>
      log.debug("[{}] <-- {}", self.path.name, msg)
      router ! msg
      
    case signal @ Wamp.Disconnected =>
      // This happens when the underlying WebSocket transport disconnects
      log.debug("[{}]     Wamp.Disconnected [{}]", self.path.name, peer.path.name)
      router ! signal
      context.stop(peer)
      context.stop(self)

    case status @ stream.Failure(ex) =>
      // This happens if disconnect-offending-peers is switched on 
      // and the connected peer sends and offending message
      log.warning("[{}]     Stream.Failure [{}: {}]", self.path.name, ex.getClass.getName, ex.getMessage)
      router ! Wamp.Disconnected
      context.stop(peer)
      context.stop(self)

    case cmd @ Wamp.Disconnect =>
      // This happens when the router commands a disconnection
      router ! Wamp.Disconnected
      context.stop(peer)
      context.stop(self)
  }
}


object ConnectionHandler {
  /**
    * Create a Props for an actor of this type
    * 
    * @param router ???
    * @return the props
    */
  def props(router: ActorRef, path: String, validateStrictUris: Boolean, disconnectOffendingPeers: Boolean) = 
    Props(new ConnectionHandler(router, path, validateStrictUris, disconnectOffendingPeers))
}
