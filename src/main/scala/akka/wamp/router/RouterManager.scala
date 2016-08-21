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

      binding.onComplete {
        case Success(b) =>
          val host = b.localAddress.getHostString
          val port = b.localAddress.getPort
          val path = context.system.settings.config.getString("akka.wamp.router.path")
          val url = s"ws://$host:$port/$path"
          router ! Wamp.Bound(url)
        case Failure(cause) =>
          router ! Wamp.BindFailed(cause)
      }
    }
  }
}
