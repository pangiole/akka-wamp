package akka.wamp

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{TextMessage, WebSocketRequest, WebSocketUpgradeResponse, Message => WebSocketMessage}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategies}
import akka.wamp.serialization.JsonSerialization

import scala.concurrent.Future


class WampManager(ser: JsonSerialization)(implicit s:ActorSystem, m: ActorMaterializer) extends Actor with ActorLogging {
  import Wamp._
  import s.dispatcher
  
  var outgoingActor: ActorRef = _
  
  
  def receive = {
    case cmd @ Connect(uri) => {
      
      val client = sender()
      log.debug("Connecting to {}", uri)

      val outgoingSource: Source[SpecifiedMessage, ActorRef] =
        Source.actorRef[SpecifiedMessage](0, OverflowStrategies.DropBuffer)

      val webSocketFlow: Flow[WebSocketMessage, WebSocketMessage, Future[WebSocketUpgradeResponse]] =
        Http().webSocketClientFlow(WebSocketRequest(uri, subprotocol = Some("wamp.2.json")))

      val serializeFlow: Flow[SpecifiedMessage, WebSocketMessage, NotUsed] =
        Flow[SpecifiedMessage]
          .log("toWebSocket")
          .map { msg =>
            TextMessage.Strict(ser.serialize(msg))
          }

      val deserializeFlow: Flow[WebSocketMessage, SpecifiedMessage, NotUsed] =
        Flow[WebSocketMessage]
          .map {
            //TODO what happens when deserialize throws Exception?
            case TextMessage.Strict(text) => ser.deserialize(text)
            case _ => ???
          }.log("fromWebSocket")

      val incomingActor = client
      val incomingSink: Sink[SpecifiedMessage, NotUsed] =
        Sink.actorRef[SpecifiedMessage](incomingActor, ConnectionClosed)


      // upgradeResponse is a Future[WebSocketUpgradeResponse] that 
      // completes or fails when the connection succeeds or fails
      val (outgoingActor, upgradeResponse) =
        outgoingSource
          .via(serializeFlow)
          .viaMat(webSocketFlow)(Keep.both) // keep the materialized Future[WebSocketUpgradeResponse]
          .viaMat(deserializeFlow)(Keep.left)
          .toMat(incomingSink)(Keep.left)
          .run()

      // hold the actor ref for later usage
      this.outgoingActor = outgoingActor
      
      // just like a regular http request we can get 404 NotFound etc.
      // that will be available from upgrade.response
      upgradeResponse.map { upgrade =>
        if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
          client.tell(Connected(outgoingActor), outgoingActor)
        } else {
          log.warning("Connection failed: {}", upgrade.response.status)
          client ! CommandFailed(cmd)
        }
      }
    }

    case m: SpecifiedMessage => {
      outgoingActor ! m
    }
  }
}
