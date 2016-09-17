package akka.wamp.router

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status => stream}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message => WebSocketMessage}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.{Route, Directives => dsl}
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategies}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Sink, Source}
import akka.wamp.messages.Message
import akka.wamp.{Wamp, messages => wamp, _}
import akka.wamp.serialization.SerializationFlows


/**
  * This router.Transport connects two [[Peer]]s and provides a WebSocket channel
  * over which JSON Messages for a [[Session]] can flow in both directions.
  *
  * @param router is the first peer that will be connected by this transport
  */
class Transport(router: ActorRef, serializationFlows: SerializationFlows) 
  extends Actor with ActorLogging 
{
  implicit val mat = ActorMaterializer()
  // TODO close the materializer at some point
  
  val websocketHandler: Flow[WebSocketMessage, WebSocketMessage, ActorRef] = {

    // A stream source that will be materialized as an actor and
    // that will emit WAMP messages being serialized out to the websocket
    val transportSource: Source[Message, ActorRef] =
      Source.
        actorRef[Message](bufferSize = 4, OverflowStrategies.Fail)

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
        val fromWebSocket = builder.add(serializationFlows.deserialize)

        // The merge junction forwards all messages fromWebSocket downstream to the transportSink
        val merge = builder.add(Merge[Wamp.AbstractMessage](2))

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
    log.debug("[{}]     Starting", self.path.name)
  }

  override def postStop(): Unit = {
    log.debug("[{}]    Stopped", self.path.name)
  }
  
  def receive: Receive = {

    case conn: Http.IncomingConnection =>
      log.debug("[{}]     Http.Incoming accepted on {}", self.path.name, conn.localAddress)
      conn.handleWith(httpFlow)
      
    case Wamp.Connected(peer) =>
      client = peer
      log.debug("[{}]     Wamp.Connected to client [{}]", self.path.name, client.path.name)

    case msg: Message if (sender() == router) =>
      log.debug("[{}] --> {}", self.path.name, msg)
      client ! msg
      
    case msg: Message =>
      log.debug("[{}] <-- {}", self.path.name, msg)
      router ! msg
      
    case Wamp.Disconnected =>
      log.debug("[{}]     Disconnected from client [{}]", self.path.name, client.path.name)
      stop()

    case stream.Failure(ex) =>
      log.warning("[{}]   Stream failure {}: {}", self.path.name, ex.getClass.getName, ex.getMessage)
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
    * @param router ???
    * @param serializationFlows ???
    * @return the props
    */
  def props(router: ActorRef, serializationFlows: SerializationFlows) = 
    Props(new Transport(router, serializationFlows))
}
