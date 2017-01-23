/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */
package akka.wamp.client

import java.net.URI

import akka.actor.{ActorSystem, Props}
import akka.wamp.{EndpointConfig, BackoffOptions}
import akka.wamp.{Peer, Uri}

import scala.concurrent.duration.{FiniteDuration, NANOSECONDS}
import scala.concurrent.{Future, Promise}

/**
  * Represents a client.
  *
  * Instances can be created using its companion object.
  *
  * {{{
  * val client = ...
  *
  * val session =
  *   for {
  *     conn <- client.connect("myrouter")
  *     session <- conn.open("myrealm")
  *   }
  *   yield(session)
  * }}}
  *
  * @see [[akka.wamp.client.japi.Client]]
  */
class Client private[client](sstm: ActorSystem) extends Peer with EndpointConfig {

  /** Is this client's actor system */
  val actorSystem: ActorSystem = sstm

  import ConnectorSupervisor._
  import actorSystem.dispatcher

  /* Is the supervisor of all connectors */
  private val supervisor = actorSystem.actorOf(Props[ConnectorSupervisor], name = "wamp.client")


  val config = actorSystem.settings.config.getConfig("akka.wamp.client")


  /**
    * Connects to a router with the ``"local"`` endpoint configuration
    *
    * @return the (future of) connection
    */
  def connect(): Future[Connection] = {
    connect(endpoint = "local")
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

  /**
    * Connects to a router at the given URL and negotiate the given message format
    *
    * @param address is the address to connect to (e.g. ``"wss://hostname:8433/router"``)
    * @param format is the format of messages as exchanged the wire (e.g. ``"msgpack"``)
    * @return a (future of) connection
    */
  def connect(address: String, format: String): Future[Connection] = {
    connect("local", Some(new URI(address)), Some(format))
  }


  private def connect(endpoint: String, address: Option[URI], format: Option[String]): Future[Connection] = {
    try {
      val (addr, fmt) = endpointConfig(endpoint, config)
      val promise = Promise[Connection]
      val cmd = SpawnConnector(
        address.getOrElse(addr),
        format.getOrElse(fmt),
        new BackoffOptions(
          FiniteDuration(config.getDuration("min-backoff").toNanos, NANOSECONDS),
          FiniteDuration(config.getDuration("max-backoff").toNanos, NANOSECONDS),
          config.getDouble("random-factor")
        ),
        promise)
      supervisor ! cmd
      promise.future
    } catch {
      case cause: Throwable =>
        Future.failed[Connection](new ClientException(cause.getMessage, cause))
    }
  }


  /**
    * Connects and then opens a new session attached to the ``"local"``
    *
    * @return the (future of) session
    */
  def open(): Future[Session] = {
    open(realm = "default", endpoint = "local")
  }

  /**
    * Connects and then opens a new session attached to the given realm.
    *
    * @param realm is the realm to attach the session to
    * @return the (future of) session
    */
  def open(realm: Uri): Future[Session] = {
    open(realm, endpoint = "local")
  }

  /**
    * Connects and then opens a new session attached to the given realm and
    * named endpoint configuration
    *
    * @param realm is the realm to attach the session to
    * @param endpoint is the name of a configured endpoint
    * @return the (future of) session
    */
  def open(realm: Uri, endpoint: String): Future[Session] = {
    connect(endpoint).flatMap(conn => conn.open(realm))
  }


  // TODO def terminate(): Future[Terminated]
}


/**
  * Factory for [[Client]] instances.
  *
  * {{{
  * import akka.actor._
  * import akka.wamp.client._
  * import com.typesafe.config._
  *
  * val config = ConfigFactory.load("my.conf")
  * val system = ActorSystem("myapp", config)
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
    * Creates a new client with the given actor system
    *
    * @param actorSystem is the actor system
    * @return a new client instance
    */
  def apply(actorSystem: ActorSystem): Client = new Client(actorSystem)
}
