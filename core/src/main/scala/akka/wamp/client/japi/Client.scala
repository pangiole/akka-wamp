/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.client.japi

import java.util.concurrent.CompletionStage

import akka.actor.ActorSystem
import akka.wamp.Uri
import akka.wamp.client.{Client => ClientDelegate, Connection => ConnectionDelegate, Session => SessionDelegate}

import scala.compat.java8.FutureConverters.toJava
import scala.concurrent.Future

/**
  * Represents a client.
  *
  * Instances can be created using its companion object.
  *
  * {{{
  * Client client = ...
  *
  * CompletionStage<Session> session =
  *   client.connect("myrouter").thenCompose(conn ->
  *     conn.open("myrealm")
  *   );
  * }}}
  *
  * @note Java API
  * @see [[akka.wamp.client.Client]]
  */
class Client private[japi](delegate: ClientDelegate) {
  import delegate.system.dispatcher

  /** Is this client configuration */
  val config = delegate.config


  /** Is this client actory system */
  val system = delegate.system

  /**
    * Connects to a router with the ``"default"`` transport configuration
    *
    * @return the (future of) connection
    */
  def connect(): CompletionStage[Connection] = 
    doConnect(delegate.connect())


  /**
    * Connects to a router with the given named transport configuration
    *
    * @param transport the name of a configured transport
    * @return the (future of) connection
    */
  def connect(transport: String): CompletionStage[Connection] = 
    doConnect(delegate.connect(transport))


  private def doConnect(fcd: => Future[ConnectionDelegate]): CompletionStage[Connection] = toJava {
    fcd.map(cd => new Connection(delegate = cd))
  }


  /**
    * Connects and then opens a new session attached to the ``"default"``
    *
    * @return the (future of) session
    */
  def open(): CompletionStage[Session] =
    doOpen(delegate.open())


  /**
    * Connects and then opens a new session attached to the given realm.
    *
    * @param realm the realm to attach the session to
    * @return the (future of) session
    */
  def open(realm: Uri): CompletionStage[Session] =
    doOpen(delegate.open(realm))

  /**
    * Connects and then opens a new session attached to the given realm and
    * named transport
    *
    * @param realm the realm to attach the session to
    * @param transport the name of a configured transport
    * @return the (future of) session
    */
  def open(transport: String, realm: Uri): CompletionStage[Session] = 
    doOpen(delegate.open(transport, realm))



  private def doOpen(fsd: => Future[SessionDelegate]): CompletionStage[Session] = toJava {
    fsd.map(sd => new Session(delegate = sd))
  }
}


/**
  * Factory for [[Client]] instances.
  *
  * {{{
  * import akka.actor.*;
  * import akka.wamp.client.japi.*;
  * import com.typesafe.config.*;
  *
  * Config config = ConfigFactory.load("my.conf");
  * ActorSystem system = ActorSystem.create("myapp", config);
  * Client client = Client.create(system);
  *
  * }}}
  *
  * The factory requires you to pass an [[http://doc.akka.io/docs/akka/current/general/actor-systems.html ActorSystem]]
  * properly named and configured with via a [[http://doc.akka.io/docs/akka/current/general/configuration.html Configuration]].
  *
  * @note Java API
  * @see [[akka.wamp.client.Client]]
  */
object Client {

  /**
    * Creates a new client with the given actor system
    *
    * @param system the actor system
    * @return a new client instance
    */
  def create(system: ActorSystem): Client = new Client(new ClientDelegate(system))
}