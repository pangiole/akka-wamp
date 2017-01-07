package akka.wamp.router

import java.net.{URI, URL}

import akka.actor._
import akka.http.scaladsl._
import akka.stream._
import akka.stream.scaladsl._
import akka.wamp._
import akka.wamp.messages._

import scala.concurrent._
import scala.util.{Failure, Success}

/**
  * INTERNAL API
  * 
  * The connection listener actor spawned by the [[Manager]]
  * each time it executes [[Bind]] commands
  */
private class ConnectionListener extends Actor {
  
  /** The execution context */
  private implicit val ec = context.system.dispatcher

  /** The actor materializer for Akka Stream */
  // TODO close the materializer at some point
  private implicit val materializer = ActorMaterializer()

  private var binding: Http.ServerBinding = _

  /** Router config **/
  private val routerConfig = context.system.settings.config.getConfig("akka.wamp.router")
  
  
  /**
    * Handle BIND and UNBIND commands
    */
  override def receive: Receive = {
    case cmd @ Bind(router, transport) => {
      val binder = sender()

      val transportConfig = routerConfig.getConfig(s"transport.$transport")
      val scheme = transportConfig.getString("scheme")
      val host = transportConfig.getString("host")
      val port = transportConfig.getInt("port")
      val file = transportConfig.getString("file")

      val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
        Http(context.system).
          bind(host, port)

      // when serverSource fails because of very dramatic situations 
      // such as running out of file descriptors or memory available to the system
      val reactToTopLevelFailures: Flow[Http.IncomingConnection, Http.IncomingConnection, _] =
        Flow[Http.IncomingConnection].
          watchTermination()((_, termination) => termination.failed.foreach { cause =>
            binder ! CommandFailed(cmd, cause)
          })

      val handleConnection: Sink[Http.IncomingConnection, Future[akka.Done]] =
        Sink.foreach { conn =>
          val handler = context.actorOf(ConnectionHandler.props(router, routerConfig, transportConfig))
          handler ! HandleHttpConnection(conn)
        }

      serverSource
        .via(reactToTopLevelFailures)
        .to(handleConnection)
        .run()
        .onComplete {
          case Success(b) =>
            this.binding = b
            assert(b.localAddress.getHostString == host)
            val port = b.localAddress.getPort
            val url = new URI(scheme, null, host, port, s"/$file", null, null)
            binder ! Bound(self, url)
            
          case Failure(cause) =>
            binder ! CommandFailed(cmd, cause)
        }
    }

    case cmd @ Unbind =>
      this.binding.unbind()
      context.stop(self)
  }
}

/**
  * INTERNAL API
  */
private[wamp] object ConnectionListener {
  /**
    * Factory for [[ConnectionListener]] instances
    */
  def props() = Props(new ConnectionListener())
}
