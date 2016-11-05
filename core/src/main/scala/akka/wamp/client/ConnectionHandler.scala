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
class ConnectionHandler(connector: ActorRef) 
  extends Actor with ActorLogging
{
  /** The execution context */
  private implicit val ec = context.system.dispatcher

  /** The actor materializer for Akka Stream */
  // TODO close the materializer at some point
  private implicit val materializer = ActorMaterializer()

  /** Client configuration */
  private val clientConfig = context.system.settings.config.getConfig("akka.wamp.client")
  
  /** The serialization flows */
  // TODO [Provide msgpack format](https://github.com/angiolep/akka-wamp/issues/12)
  private val serializationFlows = new JsonSerializationFlows(
    clientConfig.getBoolean("validate-strict-uris"),
    /*
     * NOTE
     * Clients will always disconnect on offending messages
     * No configuration setting is provided to change this behaviour.
     */
    dropOffendingMessages = false
  )

  /** The client actor */
  private var client: ActorRef = _

  /** The outlet actor */
  private var outletHandler: ActorRef = _
  
  /** The inlet actor (itself) */
  private val inletHandler: ActorRef = self

  /**
    * Handle CONNECT and DISCONNECT commands
    */
  override def receive: Receive = {
    case cmd @ Connect(url, format) =>
      context become handleConnecting(cmd)
      try {
        val outgoingSource: Source[WampMessage, ActorRef] =
          Source.actorRef[WampMessage](0, OverflowStrategies.DropBuffer)

        val webSocketFlow: Flow[WebSocketMessage, WebSocketMessage, Future[WebSocketUpgradeResponse]] =
          Http(context.system)
            .webSocketClientFlow(WebSocketRequest(url, subprotocol = Some(s"wamp.2.$format")))

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
            connector ! Connected(self)
          }
        }
      } catch {
        case ex: Throwable =>
          // NOTE:
          // It happens when the client tries to connect
          //    - to a malformed URL (e.g. "ws!malformed:9999/uri")
          connector ! CommandFailed(cmd, ex)
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
      connector ! CommandFailed(cmd, ex)
      self ! PoisonPill
      // TODO will it poison the outletHandler too?
    }
  }

  
  def handleConnected: Receive = {
    case outgoing: WampMessage if sender() == connector =>
      outletHandler forward outgoing
      
    case incoming: WampMessage =>
      // NOTE:
      // Incoming message are those received from router
      // (e.g. WELCOME, ABORT, EVENT, ...)
      connector forward incoming

    case cmd @ Disconnect if sender() == connector =>
      // NOTE:
      //    It happens when the connector (client) sends the 
      //    Disconnect command to terminate the outletHandler
      outletHandler ! PoisonPill
      sender() ! Disconnected
      inletHandler ! PoisonPill
      
    case signal @ Disconnected =>
      // NOTE:
      //    As documented for Sink.actorRef(), this signal 
      //    is sent when the outletHandler completes successful, 
      //    such as disconnection from router side
      connector ! Disconnected
      self ! PoisonPill
  }
}


/**
  * INTERNAL API
  */
private[wamp] object ConnectionHandler {
  /**
    * Factory for [[ConnectionHandler]] instances
    */
  def props(connector: ActorRef) = Props(new ConnectionHandler(connector))
}
