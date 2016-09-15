package akka.wamp.router

import akka.actor._
import akka.http.scaladsl._
import akka.stream._
import akka.stream.scaladsl._
import akka.wamp._
import akka.wamp.serialization._

import scala.concurrent._
import scala.util.{Failure, Success}

/**
  * INTERNAL API
  * 
  * The Akka IO manager actor for the WAMP router
  */
private class Manager extends Actor {
  
  /** The execution context */
  private implicit val ec = context.system.dispatcher

  /** The actor materializer for Akka Stream */
  // TODO close the materializer at some point
  private implicit val materializer = ActorMaterializer()

  /** Router configuration */
  private val config = context.system.settings.config.getConfig("akka.wamp.router")

  /**
    * The TCP interface (default is 127.0.0.1) to bind to
    */
  private val iface = config.getString("iface")

  /**
    * The TCP port number (default is 8080) to bind to
    */
  private val port = config.getInt("port")

  /**
    * The boolean switch (default is false) to validate against 
    * strict URIs rather than loose URIs
    */
  private val strictUris = config.getBoolean("validate-strict-uris")

  /** The serialization flows */
  // TODO https://github.com/angiolep/akka-wamp/issues/12
  private val serializationFlows = new JsonSerializationFlows(strictUris)

  
  /**
    * Handle BIND and UNBIND commands
    */
  override def receive: Receive = {
    case cmd @ Wamp.Bind(router) => {
      val binder = sender()
      val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
        Http(context.system).
          bind(iface, port)

      // when serverSource fails because of very dramatic situations 
      // such as running out of file descriptors or memory available to the system
      val reactToTopLevelFailures: Flow[Http.IncomingConnection, Http.IncomingConnection, _] =
        Flow[Http.IncomingConnection].
          watchTermination()((_, termination) => termination.onFailure {
            case cause => 
              binder ! Wamp.ConnectionFailed(cause)
          })

      val handleConnection: Sink[Http.IncomingConnection, Future[akka.Done]] =
        Sink.foreach { conn =>
          val transport = context.actorOf(Transport.props(router, serializationFlows))
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

    // TODO https://github.com/angiolep/akka-wamp/issues/13
    // case cmd @ Wamp.Unbind
  }
}

/**
  * INTERNAL API
  */
private[wamp] object Manager {
  /**
    * Factory for [[Manager]] instances
    */
  def props() = Props(new Manager())
}
