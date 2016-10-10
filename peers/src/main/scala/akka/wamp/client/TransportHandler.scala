package akka.wamp.client

import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ws.{Message => WebSocketMessage, _}
import akka.stream._
import akka.stream.scaladsl._
import akka.wamp.messages.{Message => WampMessage, _}
import akka.wamp.serialization._

import scala.concurrent.Future


private class TransportHandler extends Actor {

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
  private var transport: ActorRef = _
  
  /**
    * Handle CONNECT and DISCONNECT commands
    */
  override def receive: Receive = {
    case cmd @ Connect(uri, subprotocol) => {
      client = sender()
      try {
        val outgoingSource: Source[WampMessage, ActorRef] =
          Source.actorRef[WampMessage](0, OverflowStrategies.DropBuffer)

        val webSocketFlow: Flow[WebSocketMessage, WebSocketMessage, Future[WebSocketUpgradeResponse]] =
          Http(context.system)
            .webSocketClientFlow(WebSocketRequest(uri, subprotocol = Some(subprotocol)))

        val incomingSink: Sink[WampMessage, akka.NotUsed] =
          Sink.actorRef[WampMessage](client, onCompleteMessage = Disconnected)

        if (subprotocol != "wamp.2.json") 
          throw new IllegalArgumentException(s"$subprotocol is not supported") 
        
        val (outgoingActor, upgradeResponse) =
          outgoingSource
            .via(serializationFlows.serialize)
            .viaMat(webSocketFlow)(Keep.both)
            .viaMat(serializationFlows.deserialize)(Keep.left)
            .toMat(incomingSink)(Keep.left)
            .run()
        
        // hold outlet reference for later usage and watch for its termination
        transport = outgoingActor
        context.watch(transport)

        // just like a regular http request we can get 404 NotFound etc.
        // that will be available from upgrade.response
        upgradeResponse.onSuccess { case upgrade =>
          if (upgrade.response.status == SwitchingProtocols) {
            context become connected
            client ! Connected(self)
          } 
          else {
            client ! CommandFailed(cmd, new Exception(upgrade.response.toString))
          }
        }
      } catch {
        case ex: Throwable =>
          client ! CommandFailed(cmd, ex)
      }
    }
  }
  
  
  
  def connected: Receive = {
    case msg: WampMessage =>
      transport forward msg
    
    case cmd @ Disconnect =>
      transport ! PoisonPill
    
    case Terminated(actor) =>
      self ! PoisonPill
      client ! Disconnected
  }
}



/**
  * INTERNAL API
  */
private[wamp] object TransportHandler {
  /**
    * Factory for [[TransportHandler]] instances
    */
  def props() = Props(new TransportHandler())
}
