package akka.wamp

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Route.asyncHandler
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}
import akka.wamp.messages.{Hello, Welcome}

/**
  * A Router is a [[Peer]] of the roles [[Broker]] and [[Dealer]] which is responsible 
  * for generic call and event routing and do not run any application code.
  */
class Router extends Peer /* TODO with Broker*/ /* TODO with Dealer */ {

  /**
    * Map of open [[Session]]s by their ids
    */
  var sessions = Map.empty[Long, Session]
  
  def receive = handleSessions /* TODO orElse handleSubscriptions*/ /* TODO orElse handleProcedures */

  /**
    * Handle session lifecycle related messages such as: HELLO, WELCOME, ABORT and GOODBYE
    */
  def handleSessions: Receive = {
    
    case Hello(realm, details) =>
      // TODO generate unique session identifiers
      val sid = 1L
      sessions += (sid -> new Session(sid, self, sender))
      sender ! Welcome(sid, Map("roles" -> Map("dealer" -> Map())))
      
    // TODO case Goodbye  
  }
}




// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

object Router {
  /**
    * Launch the [[Router]] server process
    * 
    * @param args
    */
  def main(args: Array[String]): Unit = {
    
    implicit val system = ActorSystem("wamp")
    implicit val m = ActorMaterializer()
    implicit val ec = system.dispatcher

    val interface = "0.0.0.0"
    val port = 9999
         
    Http().bind(interface, port)
      // TODO .via(reactToTopLevelFailures)
      .to(Sink.foreach { conn =>
        // TODO system.log.debug("Incoming connection accepted from {}", conn.remoteAddress)
        conn.handleWith(Flow[HttpRequest]
          // TODO .via(reactToConnectionFailure)
          .mapAsync(parallelism = 1)(asyncHandler(
            new RequestHandler(
              system.actorOf(Props[Router])
            ).route
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

