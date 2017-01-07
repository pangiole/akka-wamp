/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.client.japi

import java.net.URI
import java.util.concurrent.CompletionStage

import akka.actor.ActorSystem
import akka.wamp.Uri
import akka.wamp.client.{Client => ClientDelegate}

import scala.compat.java8.FutureConverters.{toJava => asJava}

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
  def connect(): CompletionStage[Connection] = asJava {
    delegate.connect().map(c => new Connection(delegate = c))
  }

  /**
    * Connects to a router with the given named transport configuration
    *
    * @param transport the name of a configured transport
    * @return the (future of) connection
    */
  def connect(transport: String): CompletionStage[Connection] = asJava {
    delegate.connect(transport).map(c => new Connection(delegate = c))
  }

  /**
    * Connects to a router at the given URL and negotiate the given message format
    *
    * @param uri the address to connect to (e.g. ``"wss://hostname:8433/router"``)
    * @param format the message format to negotiate (e.g. ``"wamp.2.msgpack"``
    * @return a (future of) connection
    */
  private[client] def connect(uri: URI, format: String): CompletionStage[Connection] = asJava {
    delegate.connect(uri, format).map(c => new Connection(delegate = c))
  }


  /**
    * Connects and then opens a new session attached to the ``"default"``
    *
    * @return the (future of) session
    */
  def open(): CompletionStage[Session] = asJava {
    delegate.open().map(s => new Session(delegate = s))
  }


  /**
    * Connects and then opens a new session attached to the given realm.
    *
    * @param realm the realm to attach the session to
    * @return the (future of) session
    */
  def open(realm: Uri): CompletionStage[Session] = asJava {
    delegate.open(realm).map(s => new Session(delegate = s))
  }

  /**
    * Connects and then opens a new session attached to the given realm and
    * named transport
    *
    * @param realm the realm to attach the session to
    * @param transport the name of a configured transport
    * @return the (future of) session
    */
  def open(transport: String, realm: Uri): CompletionStage[Session] =asJava {
    delegate.open(transport, realm).map(s => new Session(delegate = s))
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