package akka.wamp

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.SwitchingProtocols
import akka.http.scaladsl.model.ws.{Message => WebSocketMessage, _}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategies}
import akka.wamp.Wamp._
import akka.wamp.serialization.Serializers
import akka.wamp.messages.{Message => WampMessage}
import akka.{Done, NotUsed}

import scala.concurrent.Future


class Manager(implicit system: ActorSystem, mat: ActorMaterializer) extends Actor with ActorLogging {
  import system.dispatcher
  
  // client -> outlet
  var outlets = Map.empty[ActorRef, ActorRef] 

  // router -> binding
  var bindings = Map.empty[ActorRef, Future[Http.ServerBinding]]
  
  def receive = {
    case cmd @ Bind(router, interface, port) => {
      val router = sender()
      
      /*TODO val reactToConnectionFailure =
        Flow[HttpRequest]
          .recover[HttpRequest] {
          case ex => throw ex
        }*/
      
      val handleConnection: Sink[Http.IncomingConnection, Future[Done]] = 
        Sink.foreach { conn =>
          router ! conn
        }

      val reactToTopLevelFailures: Flow[Http.IncomingConnection, Http.IncomingConnection, _] = 
        Flow[Http.IncomingConnection]
          .watchTermination()((_, termination) => termination.onFailure {
            case cause => router ! Failure(cause.getMessage)
          })


      val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
        Http()
          .bind(interface, port)
          //TODO .throttle()
      
      val binding: Future[Http.ServerBinding] =
        serverSource
          .via(reactToTopLevelFailures)
          .to(handleConnection)
          .run()
      
      bindings += (router -> binding)
      
      binding.onComplete {
        case util.Success(b) => 
          router ! Bound(b.localAddress)
        case util.Failure(ex) => 
          router ! CommandFailed(cmd)
      }
    }
      
    case Unbind =>
      val router = sender()
      for { binding <- bindings(router) } yield (binding.unbind())
      
      
      
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    case cmd @ Connect(client, uri, subprotocol) => {
      log.debug("Connecting to {} with {}", uri, subprotocol)

      val outgoingSource: Source[WampMessage, ActorRef] =
        Source.actorRef[WampMessage](0, OverflowStrategies.DropBuffer)

      val webSocketFlow: Flow[WebSocketMessage, WebSocketMessage, Future[WebSocketUpgradeResponse]] =
        Http().webSocketClientFlow(WebSocketRequest(uri, subprotocol = Some(subprotocol)))
        // TODO file an issue on Akka HTTP to request multiple subprotocol negotiation


      val incomingActor = client
      val incomingSink: Sink[WampMessage, NotUsed] =
        Sink.actorRef[WampMessage](incomingActor, Disconnected)


      val serializer = Serializers.streams(subprotocol)
      
      // upgradeResponse is a Future[WebSocketUpgradeResponse] that 
      // completes or fails when the connection succeeds or fails
      val (outgoingActor, upgradeResponse) =
        outgoingSource
          .via(serializer.serialize)
          .viaMat(webSocketFlow)(Keep.both) // keep the materialized Future[WebSocketUpgradeResponse]
          .viaMat(serializer.deserialize)(Keep.left)
          .toMat(incomingSink)(Keep.left)
          .run()

      // hold the outlet reference for later usage
      outlets += (client -> outgoingActor)
      
      // just like a regular http request we can get 404 NotFound etc.
      // that will be available from upgrade.response
      upgradeResponse.map { upgrade =>
        if (upgrade.response.status == SwitchingProtocols) {
          client ! Connected(outgoingActor)
        } else {
          log.warning("Connection failed: {}", upgrade.response.status)
          client ! CommandFailed(cmd)
        }
      }
    }

    case m: WampMessage => {
      outlets.get(sender()).foreach(c => c ! m)
    }
  }
}
