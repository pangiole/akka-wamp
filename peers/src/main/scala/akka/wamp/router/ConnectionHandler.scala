package akka.wamp.router

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status => stream}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message => WebSocketMessage}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.{Route, Directives => dsl}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Sink, Source}
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategies}
import akka.wamp.messages.{ManagedMessage => WampMessage, _}
import akka.wamp.serialization.JsonSerializationFlows
import com.typesafe.config.Config


/**
  * This connection connects two peers and provides a WebSocket channel
  * over which JSON messages for a session can flow in both directions.
  *
  * @param router is the first peer to connect
  * @param path
  * @param routerConfig
  */
private 
class ConnectionHandler(router: ActorRef, path: String, routerConfig: Config) 
  extends Actor 
    with ActorLogging 
{
  implicit val mat = ActorMaterializer()
  // TODO close the materializer at some point

  // TODO [Provide wamp.2.msgpack subprotocol](https://github.com/angiolep/akka-wamp/issues/12)
  val serializationFlows = new JsonSerializationFlows(
    routerConfig.getBoolean("validate-strict-uris"),
    routerConfig.getBoolean("disconnect-offending-peers")
  )
  
  /** The second peer to connect */
  var peer: ActorRef = _
  
  val websocketHandler: Flow[WebSocketMessage, WebSocketMessage, ActorRef] = {

    // A stream source that will be materialized as an actor and
    // that will emit WAMP messages being serialized out to the websocket
    val transportSource: Source[Message, ActorRef] =
      Source.
        actorRef[Message](bufferSize = 4, OverflowStrategies.Fail)

    // Create a new transportSink which delivers any message to this transportActor (self)
    val transportSink: Sink[WampMessage, NotUsed] =
      Sink.
        actorRef[WampMessage](self, onCompleteMessage = Disconnected)

    Flow.fromGraph(GraphDSL.create(transportSource) {
      implicit builder => transportSource =>
        import GraphDSL.Implicits._

        // As soon as a new WebSocket connection is established with a client
        // then the following materialized outlet:
        //   - will emit the Connected signal carrying the clientActor reference, and
        //   - will go downstream to the transportSink via a merge junction
        val onConnect = builder.materializedValue.map(client => Connected(client))

        // The fromWebSocket flow
        //   - receives incoming WebSocketMessages from the connected client, and
        //   - deserialize them to WampMessages going downstream to the transportSink
        val fromWebSocket = builder.add(serializationFlows.deserialize)

        // The merge junction forwards all messages fromWebSocket downstream to the transportSink
        val merge = builder.add(Merge[WampMessage](2))

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
        // TODO add handler for wamp.2.msgpack
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

  
  
  def receive: Receive = {

    case conn: Http.IncomingConnection =>
      log.debug("[{}]     Tcp.Incoming accepted on {}", self.path.name, conn.localAddress)
      conn.handleWith(httpFlow)
      
    case signal @ Connected(p) =>
      peer = p
      log.debug("[{}]     Connected [{}]", self.path.name, peer.path.name)
      router ! signal

    case msg: Message if (sender() == router) =>
      log.debug("[{}] --> {}", self.path.name, msg)
      peer ! msg
      
    case msg: Message =>
      log.debug("[{}] <-- {}", self.path.name, msg)
      router ! msg
      
    case signal @ Disconnected =>
      // This happens when the underlying WebSocket transport disconnects
      log.debug("[{}]     Disconnected [{}]", self.path.name, peer.path.name)
      router ! signal
      context.stop(peer)
      context.stop(self)

    case status @ stream.Failure(ex) =>
      // TODO when does this happen?
      log.warning("[{}]     Stream.Failure [{}: {}]", self.path.name, ex.getClass.getName, ex.getMessage)
      router ! Disconnected
      context.stop(peer)
      context.stop(self)

    case cmd @ Disconnect =>
      // This happens when the router commands a disconnection
      router ! Disconnected
      context.stop(peer)
      context.stop(self)
  }

  override def postStop(): Unit = {
    log.debug("[{}]     Stopped", self.path.name)
  }
}


object ConnectionHandler {
  /**
    * Create a Props for an actor of this type
    *
    * @param router
    * @param path
    * @param routerConfig
    * @return
    */
  def props(router: ActorRef, path: String, routerConfig: Config) = 
    Props(new ConnectionHandler(router, path, routerConfig))
}
