package akka.wamp.router

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message => WebSocketMessage}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.{Route, Directives => dsl}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Sink, Source}
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategies}
import akka.wamp.Wamp
import akka.wamp.messages.{Message => WampMessage}
import akka.wamp.serialization.Serializers
import akka.{Done, NotUsed}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status => stream}
import akka.http.scaladsl.model.ws.Message
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategies}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Sink, Source}
import akka.wamp.messages.Message
import akka.wamp.serialization.Serializers
import akka.wamp.{messages => wamp, _}


/**
  * A Transport connects two [[Peer]]s and provides a channel over which 
  * [[Message]]s for a [[Session]] can flow in both directions.
  *
  * @param router is the first peer that will be connected by this transport
  */
class Transport(router: ActorRef)(implicit mat: ActorMaterializer)
extends akka.wamp.TransportLike 
with Actor with ActorLogging 
{
  val websocketHandler: Flow[WebSocketMessage, WebSocketMessage, ActorRef] = {
    val serializer = Serializers.streams("wamp.2.json")

    // A stream source that will be materialized as an actor and
    // that will emit WAMP messages being serialized out to the websocket
    val transportSource: Source[WampMessage, ActorRef] =
      Source.
        actorRef[WampMessage](bufferSize = 4, OverflowStrategies.Fail)

    // Create a new transportSink which delivers any message to this transportActor (self)
    val transportSink: Sink[Wamp.AbstractMessage, NotUsed] =
      Sink.
        actorRef[Wamp.AbstractMessage](self, onCompleteMessage = Wamp.Disconnected)

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
        val fromWebSocket = builder.add(serializer.deserialize)

        // The merge junction forwards all messages fromWebSocket downstream to the transportSink
        val merge = builder.add(Merge[Wamp.AbstractMessage](2))

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

  val path = context.system.settings.config.getString("akka.wamp.router.path")
  
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
  
  /**
    * It is the second peer that will be connected by this transport
    */
  var client: ActorRef = _
  
  override def preStart(): Unit = {
    log.info("[{}]    Starting", self.path.name)
  }

  override def postStop(): Unit = {
    log.debug("[{}]   Stopped", self.path.name)
  }
  
  def receive: Receive = {

    case conn: Http.IncomingConnection =>
      log.debug("[{}]     Http.Incoming accepted on {}", self.path.name, conn.localAddress)
      conn.handleWith(httpFlow)
      
    case Wamp.Connected(peer) =>
      client = peer
      log.debug("[{}]     Wamp.Connected to client [{}]", self.path.name, client.path.name)

    case msg: wamp.Message if (sender() == router) =>
      log.debug("[{}] --> {}", self.path.name, msg)
      client ! msg
      
    case msg: wamp.Message =>
      log.debug("[{}] <-- {}", self.path.name, msg)
      router ! msg
      
    case Wamp.Disconnected =>
      log.debug("[{}] Disconnected from client [{}]", self.path.name, client.path.name)
      stop()

    case stream.Failure(ex) =>
      log.warning("[{}] Stream failure {}: {}", self.path.name, ex.getClass.getName, ex.getMessage)
      stop()
  }
  
  def stop() = {
    router ! Wamp.Disconnect
    context.stop(client)
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
