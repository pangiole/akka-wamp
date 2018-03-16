package akka.wamp.client

import java.net.URI
import javax.net.ssl.SSLContext

import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ws.{Message => WebSocketMessage, _}
import akka.stream._
import akka.stream.scaladsl._
import akka.wamp.messages.{Disconnected, ProtocolMessage => WampMessage, _}
import akka.wamp.serialization._

import scala.concurrent.Future


private[client]
class ConnectionHandler(connector: ActorRef, address: URI, format: String, sslContext: SSLContext)
  extends Actor with ActorLogging
{
  implicit val actorSystem = context.system
  implicit val executionContext = actorSystem.dispatcher
  // TODO close the materializer at some point
  implicit val materializer = ActorMaterializer()

  val config = actorSystem.settings.config
  val clientConfig = config.getConfig("akka.wamp.client")

  // TODO [Provide msgpack format](https://github.com/angiolep/akka-wamp/issues/12)
  val serializationFlows = new JsonSerializationFlows(
    clientConfig.getBoolean("validate-strict-uris"),
    /*
     * NOTE
     * Clients will always disconnect on offending messages
     * No configuration setting is provided to change this behaviour.
     */
    tolerateProtocolViolations = false
  )

  var client: ActorRef = _

  var outletHandler: ActorRef = _

  val inletHandler: ActorRef = self

  override def preStart(): Unit = {
    try {
      val outgoingSource: Source[WampMessage, ActorRef] =
        Source.actorRef[WampMessage](0, OverflowStrategies.DropBuffer)

      val request =
        WebSocketRequest(uri = address.toString, subprotocol = Some(s"wamp.2.$format"))

      val webSocketFlow: Flow[WebSocketMessage, WebSocketMessage, Future[WebSocketUpgradeResponse]] =
        address.getScheme match {
          case "ws" =>
            Http(actorSystem).webSocketClientFlow(request)
          case "wss" =>
            Http(actorSystem).webSocketClientFlow(request, connectionContext = ConnectionContext.https(sslContext))
          // TODO case s => throw new SevereException(s"Scheme $s not supported")
        }



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
      upgradeResponse.foreach { case upgrade =>
        if (upgrade.response.status == SwitchingProtocols) {
          context become handleConnected
          connector ! Connected(self, address, format)
        }
      }
    } catch {
      case ex: Throwable =>
        // NOTE:
        // It happens when the client tries to connect
        //    - to a malformed URL (e.g. "ws!malformed:9999/uri")
        connector ! CommandFailed(cmd = Connect(address, format), ex)
    }
  }

  override def receive: Receive = {
    case sig @ Status.Failure(ex) => {
      // NOTE: 
      // As documented for Sink.actorRef(), this signal is sent when  
      // the above AKKA STREAM is completed with a failure, such as
      //
      //   - akka.stream.StreamTcpException: Tcp command [Connect(...)] failed
      //   - java.lang.IllegalArgumentException: WebSocket upgrade did not finish because of 'unexpected status code: 404 Not Found'
      //
      connector ! CommandFailed(cmd = Connect(address, format), ex)
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
      
    case sig @ Disconnected =>
      // NOTE:
      //    As documented for Sink.actorRef(), this signal 
      //    is sent when the outletHandler completes successful, 
      //    such as disconnection from router side
      connector ! Disconnected
      self ! PoisonPill

    case sig @ Status.Failure(ex) =>
      // NOTE:
      // As documented for Sink.actorRef(), this signal is sent when
      // the above AKKA STREAM is completed with a failure, such as
      //
      //   - akka.http.scaladsl.model.ws.PeerClosedConnectionException: Peer closed connection with code 1011 'internal error'
      //
      connector ! Disconnected
      self ! PoisonPill
  }
}


private[wamp] object ConnectionHandler {
  def props(connector: ActorRef, address: URI, format: String, sslContext: SSLContext) =
    Props(new ConnectionHandler(connector, address, format, sslContext))
}
