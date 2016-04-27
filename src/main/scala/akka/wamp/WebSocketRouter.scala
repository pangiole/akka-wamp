package akka.wamp

import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route._
import akka.stream._
import akka.stream.scaladsl._



class WebSocketRouter {
  def start() = {
    implicit val system = ActorSystem("wamp")
    implicit val m = ActorMaterializer()
    implicit val ec = system.dispatcher

    val agent = system.settings.config.getString("akka.wamp.agent")
    val iface = system.settings.config.getString("akka.wamp.iface")
    val port = system.settings.config.getInt("akka.wamp.port")
    
    val router = system.actorOf(Router.props())

    Http().bind(iface, port)
      // TODO .via(reactToTopLevelFailures)
      .to(Sink.foreach { conn =>
        // TODO system.log.debug("Incoming connection accepted from {}", conn.remoteAddress)
        conn.handleWith(Flow[HttpRequest]
          // TODO .via(reactToConnectionFailure)
          .mapAsync(parallelism = 1)(asyncHandler(
          new WebSocketHandler(router).route
        ))
        )
      })
      .run()
      .onFailure {
        case ex: Throwable =>
          system.log.error(ex, "Failed to bind to {}:{}!", iface, port)
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
  
  // TODO def stop() = { ??? }
}



object WebSocketRouter {
  def main(args: Array[String]): Unit = {
    new WebSocketRouter().start()
  }
}
