package akka.wamp.client

import akka.NotUsed
import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.SwitchingProtocols
import akka.http.scaladsl.model.ws.{WebSocketRequest, WebSocketUpgradeResponse, Message => WebSocketMessage}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategies}
import akka.wamp.Wamp
import akka.wamp.messages.{Message => WampMessage}
import akka.wamp.serialization.Serializers

import scala.concurrent.Future


private[wamp] class ClientManager(implicit system: ActorSystem, mat: ActorMaterializer) extends Actor {
  implicit val ec = system.dispatcher

  // inlet -> outlet
  var outlets = Map.empty[ActorRef, ActorRef]

  override def receive: Receive = {
    case cmd@Wamp.Connect(client, uri, subprotocol) => {
      try {
        val outgoingSource: Source[WampMessage, ActorRef] =
          Source.actorRef[WampMessage](0, OverflowStrategies.DropBuffer)

        val webSocketFlow: Flow[WebSocketMessage, WebSocketMessage, Future[WebSocketUpgradeResponse]] =
          Http().webSocketClientFlow(WebSocketRequest(uri, subprotocol = Some(subprotocol)))

        val incomingSink: Sink[WampMessage, NotUsed] =
          Sink.actorRef[WampMessage](client, onCompleteMessage = Wamp.Disconnected)

        val serializer = Serializers.streams(subprotocol)

        // upgradeResponse is a Future[WebSocketUpgradeResponse] that 
        // completes or fails when the connection succeeds or fails
        val (outgoingActor, upgradeResponse) =
          outgoingSource
            .via(serializer.serialize)
            .viaMat(webSocketFlow)(Keep.both)
            .viaMat(serializer.deserialize)(Keep.left)
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

    case msg: WampMessage => {
      outlets.get(sender()).foreach(client => client ! msg)
    }
  }
}