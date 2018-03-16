/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */
package akka.wamp.client

import java.net.URI

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.wamp.{BackoffOptions, Endpoint, Peer, Uri}
import com.typesafe.config.Config

import scala.concurrent.duration.{FiniteDuration, NANOSECONDS}
import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * Represents a client.
  *
  * Instances can be created using its companion object.
  *
  * {{{
  * val system = ActorSystem()
  * val client = Client(system)
  *
  * val session =
  *   for {
  *     conn    <- client.connect("myendpoint")
  *     session <- conn.open("myrealm")
  *   }
  *   yield(session)
  * }}}
  *
  * @see [[akka.wamp.client.japi.Client]]
  */
class Client private[wamp](config: Config, supervisor: ActorRef)(implicit val executionContext: ExecutionContext) extends Peer {

  /**
    * Connects to a router with the ``"default"`` endpoint configuration
    *
    * @return the (future of) connection
    */
  def connect(): Future[Connection] = {
    connect(endpoint = "default")
  }

  /**
    * Connects to a router with the given named endpoint configuration
    *
    * @param endpoint is the name of the configured endpoint to connect to
    * @return the (future of) connection
    */
  def connect(endpoint: String): Future[Connection] = {
    connect(endpoint, address = None, format = None)
  }


  private[wamp] def connect(address: String, format: String): Future[Connection] = {
    connect("default", Some(new URI(address)), Some(format))
  }


  private def connect(endpoint: String, address: Option[URI], format: Option[String]): Future[Connection] = {
    import ConnectionSupervisor._
    try {
      val promise = Promise[Connection]
      val (_, defaultAddress, defaultFormat) = Endpoint.fromConfig(config, endpoint).toTuple
      supervisor ! SpawnWorker(
        address.getOrElse(defaultAddress),
        format.getOrElse(defaultFormat),
        new BackoffOptions(
          FiniteDuration(config.getDuration("min-backoff").toNanos, NANOSECONDS),
          FiniteDuration(config.getDuration("max-backoff").toNanos, NANOSECONDS),
          config.getDouble("random-factor")
        ),
        promise
      )
      promise.future
    } catch {
      case cause: Throwable =>
        Future.failed[Connection](new ClientException(cause.getMessage, cause))
    }
  }


  /**
    * Connects and then opens a new session joined to the ``"default"``
    *
    * @return the (future of) session
    */
  def open(): Future[Session] = {
    open(realm = "default", endpoint = "default")
  }

  /**
    * Connects and then opens a new session joined to the given realm.
    *
    * @param realm is the realm to join the session to
    * @return the (future of) session
    */
  def open(realm: Uri): Future[Session] = {
    open(realm, endpoint = "default")
  }

  /**
    * Connects and then opens a new session joined to the given realm and
    * named endpoint configuration
    *
    * @param realm is the realm to join the session to
    * @param endpoint is the name of a configured endpoint
    * @return the (future of) session
    */
  def open(realm: Uri, endpoint: String): Future[Session] = {
    connect(endpoint).flatMap(conn => conn.open(realm))
  }


  // TODO def terminate(): Future[Terminated]
}


/**
  * Factory for the [[Client]] instance.
  *
  * {{{
  * import akka.actor._
  * import akka.wamp.client._
  *
  * val system = ActorSystem()
  * val client = Client(system)
  * }}}
  *
  * The client factory requires you to pass an [[http://doc.akka.io/docs/akka/current/general/actor-systems.html ActorSystem]]
  * properly named and configured via a [[http://doc.akka.io/docs/akka/current/general/configuration.html Configuration]].
  *
  * @see [[akka.wamp.client.japi.Client]]
  */
object Client {
  /**
    * Creates the client with the given actor system
    *
    * @param system is the actor system
    * @return a new client instance
    */
  def apply(system: ActorSystem): Client = {
    val config = system.settings.config.getConfig("akka.wamp.client")
    val supervisor = system.actorOf(Props[ConnectionSupervisor])
    val executionContext = system.dispatcher
    new Client(config, supervisor)(executionContext)
  }
}
