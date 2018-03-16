package akka.wamp.router

import java.net.URI

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Status => stream}
import akka.http.scaladsl.model.ws.{Message => WebSocketMessage}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Sink, Source}
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategies}
import akka.wamp.messages.{Message => WampMessage, _}
import akka.wamp.serialization.JsonSerializationFlows
import com.typesafe.config.Config


private[router]
class ConnectionHandler(routerRef: ActorRef, uri: URI, format: String, routerConfig: Config) extends Actor  with ActorLogging
{
  implicit val mat = ActorMaterializer()
  // TODO close the materializer at some point

  val webroot = routerConfig.getString("webroot")

  // TODO [Provide msgpack format](https://github.com/angiolep/akka-wamp/issues/12)
  val serializationFlows = new JsonSerializationFlows(
    routerConfig.getBoolean("validate-strict-uris"),
    routerConfig.getBoolean("tolerate-protocol-violations")
  )



  val websocketHandler: Flow[WebSocketMessage, WebSocketMessage, ActorRef] = {

    // A stream source that will be materialized as an actor and
    // that will emit WAMP messages being serialized out to the websocket
    val transportSource: Source[ProtocolMessage, ActorRef] =
      Source.
        actorRef[ProtocolMessage](bufferSize = 4, OverflowStrategies.Fail)

    // Create a new transportSink which delivers any message to this transportActor (self)
    val transportSink: Sink[WampMessage, NotUsed] =
      Sink.actorRef[WampMessage](self, onCompleteMessage = Disconnected)

    Flow.fromGraph(GraphDSL.create(transportSource) {
      implicit builder => transportSource =>
        import GraphDSL.Implicits._

        // As soon as a new WebSocket connection is established with the remote peer
        // then the following materialized outlet:
        //   - will emit the Connected signal conveying the peer actor reference, and
        //   - will go downstream to the transportSink via a merge junction
        val onConnect = builder.materializedValue.map(peer => Connected(peer, uri, format))

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
    get {
      path(uri.getPath.drop(1)) {
        handleWebSocketMessagesForProtocol(websocketHandler, s"wamp.2.$format")
        // TODO add handler for msgpack
      }
    } ~
    getFromDirectory(webroot)
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


  /** The second peer to connect */
  var peer: ActorRef = _

  def receive: Receive = {
    case cmd @ HandleHttpConnection(conn) =>
      log.debug("[{}]     Handling HTTP connection {}", self.path.name, conn.localAddress)
      conn.handleWith(httpFlow)

    case signal @ Connected(p, url, format) =>
      peer = p
      log.debug("[{}]     Connected WAMP [{}]", self.path.name, peer.path.name)
      routerRef ! Connected(self, url, format)

    case msg: ProtocolMessage if (sender() == routerRef) =>
      log.debug("[{}] --> {}", self.path.name, msg)
      peer ! msg

    case msg: ProtocolMessage /* if (sender() == peer) */ =>
      log.debug("[{}] <-- {}", self.path.name, msg)
      routerRef ! msg

    case signal @ Disconnected =>
      // NOTE:
      //    It happens when the AKKA STREAM completes
      //    (e.g. when the underlying WebSocket disconnects
      //          from client side)
      log.debug("[{}] !!! Disconnected [{}]", self.path.name, peer.path.name)
      routerRef ! Disconnected
      self ! PoisonPill

    case status @ stream.Failure(ex) =>
      // NOTE:
      //    It happens when exceptions are thrown and
      //    the above AKKA STREAM completes with failure
      log.warning("[{}] !!! Stream.Failure [{}: {}]", self.path.name, ex.getClass.getName, ex.getMessage)
      routerRef ! Disconnected
      self ! PoisonPill

    case cmd @ Disconnect =>
      // NOTE:
      //    It happens when the router commands disconnection
      //    (e.g. upon receive offending messages)
      peer ! PoisonPill
      //    ... and Disconnected signal will 
      //    be emitted as consequence.
  }

  override def postStop(): Unit = {
    log.debug("[{}]     Stopped", self.path.name)
  }
}


private[router]
object ConnectionHandler {
  def props(router: ActorRef, uri: URI, format: String, routerConfig: Config) =
    Props(new ConnectionHandler(router, uri, format, routerConfig))
}
