package akka.wamp

import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.stream._
import akka.stream.scaladsl._
import akka.actor._


class Router extends Peer


object Router {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("wamp")
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher


    /*val reactToConnectionFailure = Flow[HttpRequest]
      .transform { () =>
        new PushStage[HttpRequest, HttpRequest] {
          override def onPush(elem: HttpRequest, ctx: Context[HttpRequest]) =
            ctx.push(elem)
          override def onUpstreamFailure(cause: Throwable, ctx: Context[HttpRequest]) = {
            // handle the failure somehow
            super.onUpstreamFailure(cause, ctx)
          }
        }
      }*/

    val route =
      get {
        pathSingleSlash {
          complete("WELCOME")
        } ~
        path("ping") {
          complete("PONG!")
        } ~
        path("crash") {
          sys.error("BOOM!")
        }
      }

    // : Flow[HttpRequest, HttpResponse, _]
    val requestHandler = Flow[HttpRequest]
      // TODO .via(reactToConnectionFailure)
      .mapAsync(parallelism = 1)(Route.asyncHandler(route))
      // TODO Route.handlerFlow(route)

    /* TODO val reactToTopLevelFailures = Flow[IncomingConnection]
      .transform { () =>
        new PushStage[IncomingConnection, IncomingConnection] {
          override def onPush(elem: IncomingConnection, ctx: Context[IncomingConnection]) =
            ctx.push(elem)
          override def onUpstreamFailure(cause: Throwable, ctx: Context[IncomingConnection]) = {
            failureMonitor ! cause
            super.onUpstreamFailure(cause, ctx)
          }
        }
      }*/


    val (host, port) = ("localhost", 9999)

    // : Source[IncomingConnection, Future[Http.ServerBinding]]
    val source = Http().bind(host, port)


    // : RunnableGraph[Future[Http.ServerBinding]]
    val graph = source
      // TODO .via(reactToTopLevelFailures)
      .to(Sink.foreach { conn =>
        // TODO log.debug("Incoming connection accepted from {}", conn.remoteAddress)
        conn.handleWith(requestHandler)
      })


    // : Future[Http.ServerBinding]
    val binding = graph.run()
    binding.onFailure {
      case ex: Throwable =>
        // TODO log.error(ex, "Failed to bind to {}:{}!", host, port)
        println(ex)
    }
  }
}

