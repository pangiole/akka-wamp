package akka.wamp.client

import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ws.{Message => WebSocketMessage, _}
import akka.stream._
import akka.stream.scaladsl._
import akka.wamp._
import akka.wamp.messages.Message
import akka.wamp.serialization._

import scala.concurrent.Future

/**
  * INTERNAL API
  * 
  * The Akka IO manager actor for the WAMP client
  */
private class Manager extends Actor {

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
  private val strictUris = config.getBoolean("validate-strict-uris")

  /** The serialization flows */
  // TODO https://github.com/angiolep/akka-wamp/issues/12
  private val serializationFlows = new JsonSerializationFlows(strictUris)
  
  // inlet -> outlet
  private var outlets = Map.empty[ActorRef, ActorRef]

  /**
    * Handle CONNECT and DISCONNECT commands
    */
  override def receive: Receive = {
    case cmd @ Wamp.Connect(client, uri, subprotocol) => {
      try {
        val outgoingSource: Source[Message, ActorRef] =
          Source.actorRef[Message](0, OverflowStrategies.DropBuffer)

        val webSocketFlow: Flow[WebSocketMessage, WebSocketMessage, Future[WebSocketUpgradeResponse]] =
          Http(context.system)
            .webSocketClientFlow(WebSocketRequest(uri, subprotocol = Some(subprotocol)))

        val incomingSink: Sink[Message, akka.NotUsed] =
          Sink.actorRef[Message](client, onCompleteMessage = Wamp.Disconnected)

        if (subprotocol != "wamp.2.json") 
          throw new IllegalArgumentException(s"$subprotocol is not supported") 
        
        val (outgoingActor, upgradeResponse) =
          outgoingSource
            .via(serializationFlows.serialize)
            .viaMat(webSocketFlow)(Keep.both)
            .viaMat(serializationFlows.deserialize)(Keep.left)
            .toMat(incomingSink)(Keep.left)
            .run()

        // hold the outlet reference for later usage
        outlets += (client -> outgoingActor)

        // just like a regular http request we can get 404 NotFound etc.
        // that will be available from upgrade.response
        upgradeResponse.onSuccess { case upgrade =>
          if (upgrade.response.status == SwitchingProtocols) {
            client ! Wamp.Connected(outgoingActor)
          } else {
            client ! Wamp.ConnectionFailed(new Exception(upgrade.response.toString))
          }
        }

        upgradeResponse.onFailure {
          case ex: Throwable =>
            client ! Wamp.ConnectionFailed(ex)
        }
      } catch {
        case ex: Throwable =>
          client ! Wamp.ConnectionFailed(ex)
      }
    }

    // TODO https://github.com/angiolep/akka-wamp/issues/29
    // case cmd @ Wamp.Disconnect
      
    case msg: Message => {
      outlets.get(sender()).foreach(client => client ! msg)
    }
  }
}


/**
  * INTERNAL API
  */
private[wamp] object Manager {
  /**
    * Factory for [[Manager]] instances
    */
  def props() = Props(new Manager())
}
