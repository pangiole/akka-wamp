package akka.wamp

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Route.asyncHandler
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}

/**
  * A Router is a [[Peer]] of the roles [[Broker]] and [[Dealer]] 
  * which is responsible for generic call and event routing 
  * and do not run any application code.
  */
class Router extends Peer




object Router {
  /**
    * Launch the [[Router]] server process
    * 
    * @param args
    */
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("wamp")
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher

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
    
    val peer = new Router

    
    val flowHandler = Flow[HttpRequest]
      // TODO .via(reactToConnectionFailure)
      .mapAsync(parallelism = 1)(asyncHandler(WebsocketTransport(peer).route))
    
      
    val (host, port) = ("localhost", 9999)

    // : Source[IncomingConnection, Future[Http.ServerBinding]]
    val connSource = Http().bind(host, port)


    // : RunnableGraph[Future[Http.ServerBinding]]
    val graph = connSource
      // TODO .via(reactToTopLevelFailures)
      .to(Sink.foreach { conn =>
        system.log.debug("Incoming connection accepted from {}", conn.remoteAddress)
        conn.handleWith(flowHandler)
      })


    // : Future[Http.ServerBinding]
    val binding = graph.run()
    
    binding.onFailure {
      case ex: Throwable =>
        system.log.error(ex, "Failed to bind to {}:{}!", host, port)
    }
  }
}

