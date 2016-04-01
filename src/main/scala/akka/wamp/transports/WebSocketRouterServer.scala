package akka.wamp.transports

import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route._
import akka.stream._
import akka.stream.scaladsl._
import akka.wamp.Router

object WebSocketRouterServer {

  /**
    * Launch the [[Router]] server process
    *
    * @param args
    */
  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("wamp")
    implicit val m = ActorMaterializer()
    implicit val ec = system.dispatcher

    val router = system.actorOf(Router.props())

    val interface = "127.0.0.1"
    val port = 9999

    Http().bind(interface, port)
      // TODO .via(reactToTopLevelFailures)
      .to(Sink.foreach { conn =>
        // TODO system.log.debug("Incoming connection accepted from {}", conn.remoteAddress)
        conn.handleWith(Flow[HttpRequest]
          // TODO .via(reactToConnectionFailure)
          .mapAsync(parallelism = 1)(asyncHandler(
            new HttpRequestHandler(router).route
          ))
        )
      })
      .run()
      .onFailure {
        case ex: Throwable =>
          system.log.error(ex, "Failed to bind to {}:{}!", interface, port)
      }


    /*
    val failureMonitor = system.actorOf(MyExampleMonitoringActor.props)
    
    val reactToTopLevelFailures = Flow[IncomingConnection]
      .watchTermination()((_, termination) => termination.onFailure {
        case cause => failureMonitor ! cause
      })
    */

    /*
    val reactToConnectionFailure = Flow[HttpRequest]
      .recover[HttpRequest] {
      case ex =>
        // handle the failure somehow
        throw ex
    }
    */
  }
}
