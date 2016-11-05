package akka.wamp.router

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

      val webroot = routerConfig.getString("webroot")
      val transportConfig = routerConfig.getConfig(s"transport.$transport")
      val protocol = transportConfig.getString("protocol")
      val format = transportConfig.getString("format")
      val iface = transportConfig.getString("iface")
      val port = transportConfig.getInt("port")
      val upath = transportConfig.getString("upath")
      
      val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
        Http(context.system).
          bind(iface, port)

      // when serverSource fails because of very dramatic situations 
      // such as running out of file descriptors or memory available to the system
      val reactToTopLevelFailures: Flow[Http.IncomingConnection, Http.IncomingConnection, _] =
        Flow[Http.IncomingConnection].
          watchTermination()((_, termination) => termination.onFailure {
            case cause => 
              binder ! CommandFailed(cmd, cause)
          })

      val handleConnection: Sink[Http.IncomingConnection, Future[akka.Done]] =
        Sink.foreach { conn =>
          val handler = context.actorOf(ConnectionHandler.props(router, routerConfig, upath, webroot))
          handler ! HandleHttpConnection(conn)
        }

      serverSource
        .via(reactToTopLevelFailures)
        .to(handleConnection)
        .run()
        .onComplete {
          case Success(b) =>
            this.binding = b
            assert(b.localAddress.getHostString == iface)
            val port = b.localAddress.getPort
            binder ! Bound(self, port)
            
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
