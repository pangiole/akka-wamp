package akka.wamp.client

import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ws.{Message => WebSocketMessage, _}
import akka.stream._
import akka.stream.scaladsl._
import akka.wamp.messages.{Disconnected, Message => WampMessage, _}
import akka.wamp.serialization._

import scala.concurrent.Future


private 
class TransportHandler(clientRef: ActorRef) 
  extends Actor with ActorLogging
{
  /** The execution context */
  private implicit val ec = context.system.dispatcher

  /** The actor materializer for Akka Stream */
  // TODO close the materializer at some point
  private implicit val materializer = ActorMaterializer()

  /** Client configuration */
  private val config = context.system.settings.config.getConfig("akka.wamp.client")

  /**
    * The boolean switch (default is false) to validate against 
    * strict URIs rather than loose URIs
    */
  private val validateStrictUris = config.getBoolean("validate-strict-uris")

  /**
    * The boolean switch to disconnect those peers that 
    * send invalid messages.
    */
  private val disconnectOffendingPeers = config.getBoolean("disconnect-offending-peers")

  /** The serialization flows */
  // TODO [Provide wamp.2.msgpack subprotocol](https://github.com/angiolep/akka-wamp/issues/12)
  private val serializationFlows = new JsonSerializationFlows(validateStrictUris, disconnectOffendingPeers)

  /** The client actor */
  private var client: ActorRef = _

  /** The outlet actor */
  private var outletHandler: ActorRef = _

  /**
    * Handle CONNECT and DISCONNECT commands
    */
  override def receive: Receive = {
    case cmd @ Connect(uri, subprotocol) =>
      context become handleConnecting(cmd)
      try {
        val outgoingSource: Source[WampMessage, ActorRef] =
          Source.actorRef[WampMessage](0, OverflowStrategies.DropBuffer)

        val webSocketFlow: Flow[WebSocketMessage, WebSocketMessage, Future[WebSocketUpgradeResponse]] =
          Http(context.system)
            .webSocketClientFlow(WebSocketRequest(uri, subprotocol = Some(subprotocol)))

        val incomingSink: Sink[WampMessage, akka.NotUsed] =
          Sink.actorRef[WampMessage](self, onCompleteMessage = Disconnected)

        // AKKA STREAM
        val (outgoingActor, upgradeResponse) =
          outgoingSource
            .via(serializationFlows.serialize)
            .viaMat(webSocketFlow)(Keep.both)
            .viaMat(serializationFlows.deserialize)(Keep.left)
            .toMat(incomingSink)(Keep.left)
            .run()

        // hold outlet reference for later usage
        outletHandler = outgoingActor

        // just like a regular http request we can get 404 NotFound etc.
        // that will be available from upgrade.response
        upgradeResponse.onSuccess { case upgrade =>
          if (upgrade.response.status == SwitchingProtocols) {
            context become handleConnected
            clientRef ! Connected(self)
          }
        }
      } catch {
        case ex: Throwable =>
          // NOTE:
          // It happens when the client tries to connect
          //    - to a malformed URL (e.g. "ws!malformed:9999/uri")
          clientRef ! CommandFailed(cmd, ex)
      }
  }

  
  def handleConnecting(cmd: Connect): Receive = {
    case signal @ Status.Failure(ex) => {
      // NOTE: 
      // As documented for Sink.actorRef(), this signal is sent when  
      // the above AKKA STREAM is completed with a failure, such as
      //
      //   - akka.stream.StreamTcpException: Tcp command [Connect(...)] failed
      //   - java.lang.IllegalArgumentException: WebSocket upgrade did not finish because of 'unexpected status code: 404 Not Found'
      //
      clientRef ! CommandFailed(cmd, ex)
      self ! PoisonPill
      // TODO will it poison outletHandler too?
    }
  }

  
  def handleConnected: Receive = {
    case outgoing: WampMessage if sender() == clientRef =>
      outletHandler forward outgoing
      
    case incoming: WampMessage =>
      // NOTE:
      // Incoming message are those received from router
      // (e.g. WELCOME, ABORT, EVENT, ...)
      clientRef forward incoming

    case cmd @ Disconnect =>
      // NOTE:
      // It happens when client send Disconnect to this handler
      // so to successfully terminate the outletHandler
      // RouterDisconnect will be sent as consequence
      outletHandler ! PoisonPill
      
    case signal @ Disconnected =>
      // As documented for Sink.actorRef(), this signal is sent when  
      // the above AKKA STREAM is completed successful, such as 
      // disconnection from router side
      clientRef ! Disconnected
      self ! PoisonPill
  }
}


/**
  * INTERNAL API
  */
private[wamp] object TransportHandler {
  /**
    * Factory for [[TransportHandler]] instances
    */
  def props(clientRef: ActorRef) = Props(new TransportHandler(clientRef))
}
