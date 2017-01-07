/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */
package akka.wamp.client

import java.net.URI

import akka.actor.{ActorSystem, Props}
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
class Client private[client](sstm: ActorSystem) extends Peer {

  /** Is this client's actor system */
  val system: ActorSystem = sstm

  import ConnectorSupervisor._
  import system.dispatcher

  /* Is the supervisor of all connectors */
  private val supervisor = system.actorOf(Props[ConnectorSupervisor], name = "wamp.client")

  /**
    * Is this client configuration
    */
  val config = system.settings.config.getConfig("akka.wamp.client")


  /**
    * Connects to a router with the ``"default"`` transport configuration
    * 
    * @return the (future of) connection
    */
  def connect(): Future[Connection] = {
    connect(transport = "default")
  }

  /**
    * Connects to a router with the given named transport configuration
    *
    * @param transport the name of a configured transport
    * @return the (future of) connection
    */
  def connect(transport: String): Future[Connection] = {
    connect(transport, url = None, format = None)
  }

  /**
    * Connects to a router at the given URL and negotiate the given message format
    *
    * @param uri the address to connect to (e.g. ``"wss://hostname:8433/router"``)
    * @param format the message format to negotiate (e.g. ``"wamp.2.msgpack"``
    * @return a (future of) connection
    */
  private[client] def connect(uri: URI, format: String): Future[Connection] = {
    connect("default", Some(uri), Some(format))
  }


  private def connect(transport:String, url: Option[URI], format: Option[String]): Future[Connection] = {
    try {
      val transportConfig = config.getConfig(s"transport.$transport")
      connect(
        url.getOrElse(???/*transportConfig.getString("url")*/),
        format.getOrElse(transportConfig.getString("format")),
        new BackoffOptions(
          FiniteDuration(transportConfig.getDuration("min-backoff").toNanos, NANOSECONDS),
          FiniteDuration(transportConfig.getDuration("max-backoff").toNanos, NANOSECONDS),
          transportConfig.getDouble("random-factor")
        )
      )
    } catch {
      case cause: Throwable =>
        Future.failed[Connection](new ClientException(cause.getMessage, cause))
    }
  }


  private def connect(uri: URI, format: String, options: BackoffOptions): Future[Connection] =  {
    val promise = Promise[Connection]
    supervisor ! SpawnConnector(uri, format, options, promise)
    // NOTE: the promise will be fulfilled upon connection establishment
    promise.future
  }

  /**
    * Connects and then opens a new session attached to the ``"default"``
    *
    * @return the (future of) session
    */
  def open(): Future[Session] = {
    open("default", transport = "default")
  }

  /**
    * Connects and then opens a new session attached to the given realm.
    *
    * @param realm the realm to attach the session to
    * @return the (future of) session
    */
  def open(realm: Uri): Future[Session] = {
    open(realm, transport = "default")
  }

  /**
    * Connects and then opens a new session attached to the given realm and
    * named transport
    *
    * @param realm the realm to attach the session to
    * @param transport the name of a configured transport
    * @return the (future of) session
    */
  def open(realm: Uri, transport: String): Future[Session] = {
    connect(transport).flatMap(conn => conn.open(realm))
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
    * @param system the actor system
    * @return a new client instance
    */
  def apply(system: ActorSystem): Client = new Client(system)


}
