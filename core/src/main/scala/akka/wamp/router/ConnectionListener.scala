package akka.wamp.router

import java.net.URI
import javax.net.ssl.SSLContext

import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}
import akka.wamp.EndpointConfig
import akka.wamp.messages.{Bind, CommandFailed, HandleHttpConnection, Unbind, Bound}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/*
 * Is the connection listener actor spawned by the [[Manager]]
 * each time it executes the [[Bind]] command
 */
private[router]
class ConnectionListener(binder: ActorRef, router: ActorRef, endpoint: String, sslContext: SSLContext)
  extends Actor with EndpointConfig {

  implicit val actorSystem = context.system
  implicit val executionContext = actorSystem.dispatcher
  // TODO close the materializer at some point
  implicit val materializer = ActorMaterializer()

  val config = actorSystem.settings.config.getConfig("akka.wamp.router")
  val (address, format) = endpointConfig(endpoint, config)

  // actor mutable variables
  var serverBinding: Option[Http.ServerBinding] = None
  var boundURI: URI = _


  override def preStart(): Unit = {
    val connectionSource =
      address.getScheme match {
        case "ws" =>
          Http(actorSystem).bind(address.getHost, address.getPort)
        case "wss" =>
          Http(actorSystem).bind(address.getHost, address.getPort, connectionContext = ConnectionContext.https(sslContext))
        case scheme =>
          // TODO write a test for this scenario
          throw new Exception(s"Scheme $scheme not supported")
      }

    // when serverSource fails because of very dramatic situations
    // such as running out of file descriptors or memory available to the system
    val reactToTopLevelFailures: Flow[Http.IncomingConnection, Http.IncomingConnection, _] =
    Flow[Http.IncomingConnection].
      watchTermination()((_, termination) => termination.failed.foreach { ex =>
        binder ! CommandFailed(cmd = Bind(router, endpoint), ex)
      })

    val handleConnection: Sink[Http.IncomingConnection, Future[akka.Done]] =
      Sink.foreach { conn =>
        val handler = context.actorOf(ConnectionHandler.props(router, boundURI, format, config))
        handler ! HandleHttpConnection(conn)
      }

    connectionSource
      .via(reactToTopLevelFailures)
      .to(handleConnection)
      .run()
      .onComplete {
        case Success(binding) =>
          this.serverBinding = Some(binding)
          this.boundURI = new URI(address.getScheme, null, binding.localAddress.getHostString, binding.localAddress.getPort, address.getPath, null, null)
          binder ! Bound(self, boundURI)

        case Failure(ex) =>
          binder ! CommandFailed(cmd = Bind(router, endpoint), ex)
      }
  }


  override def receive: Receive = {
    case cmd @ Unbind =>
      this.serverBinding.foreach(b => {
        b.unbind()
        context.stop(self)
      })
  }


  /*
  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException         => Stop
    case _: DeathPactException           => Stop
    case _: Exception                    => Restart
  }
  */

}



private[wamp] object ConnectionListener {
  def props(binder: ActorRef, router: ActorRef, endpoint: String, sslContext: SSLContext) =
    Props(new ConnectionListener(binder, router, endpoint, sslContext))
}
