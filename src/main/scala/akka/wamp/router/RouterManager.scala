package akka.wamp.router

import akka.Done
import akka.actor.{Actor, ActorSystem}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.wamp.Wamp

import scala.concurrent.Future
import scala.util.{Failure, Success}


private[wamp] class RouterManager(implicit system: ActorSystem, mat: ActorMaterializer) extends Actor  {
  implicit val ec = system.dispatcher

  val iface = system.settings.config.getString("akka.wamp.router.iface")
  
  val port = system.settings.config.getInt("akka.wamp.router.port")
  
  override def receive: Receive = {
    case cmd @ Wamp.Bind(router) => {
      val binder = sender()
      val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
        Http().
          bind(iface, port)
          //TODO throttle()

      // when serverSource fails because of very dramatic situations 
      // such as running out of file descriptors or memory available to the system
      val reactToTopLevelFailures: Flow[Http.IncomingConnection, Http.IncomingConnection, _] =
        Flow[Http.IncomingConnection].
          watchTermination()((_, termination) => termination.onFailure {
            case cause => 
              binder ! Wamp.ConnectionFailed(cause)
          })

      val handleConnection: Sink[Http.IncomingConnection, Future[Done]] =
        Sink.foreach { conn =>
          val transport = context.actorOf(Transport.props(router))
          transport ! conn
        }

      val binding: Future[Http.ServerBinding] =
        serverSource.
          via(reactToTopLevelFailures).
          to(handleConnection).
          run()

      // TODO bindings += (router -> binding)

      binding.onComplete {
        case Success(b) =>
          router ! Wamp.Bound(b.localAddress)
        case Failure(cause) =>
          router ! Wamp.BindFailed(cause)
      }
    }
  }
}
