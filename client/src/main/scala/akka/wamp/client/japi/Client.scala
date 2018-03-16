/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.client.japi

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
  import delegate.executionContext

  /**
    * Connects to a router listening at the ``"default"`` endpoint
    *
    * @return the (future of) connection
    */
  def connect(): CompletionStage[Connection] = asJava {
    delegate.connect().map(c => new Connection(delegate = c))
  }

  /**
    * Connects to a router listening at the given endpoint
    *
    * @param endpoint is the name of a configured endpoint
    * @return the (future of) connection
    */
  def connect(endpoint: String): CompletionStage[Connection] = asJava {
    delegate.connect(endpoint).map(c => new Connection(delegate = c))
  }

  /**
    * Connects to a router listening at the given address and
    * providing the given message format
    *
    * @param address is the address to connect to (e.g. ``"wss://host.net:8433/router"``)
    * @param format is the message format to negotiate (e.g. ``"msgpack"``
    * @return a (future of) connection
    */
  @deprecated("Prefer connect(endpoint: String) instead", "0.16")
  def connect(address: String, format: String): CompletionStage[Connection] = asJava {
    delegate.connect(address, format).map(c => new Connection(delegate = c))
  }


  /**
    * Connects and then opens a new session joined to the ``"default"`` realm
    *
    * @return the (future of) session
    */
  def open(): CompletionStage[Session] = asJava {
    delegate.open().map(s => new Session(delegate = s))
  }


  /**
    * Connects and then opens a new session joined to the given realm.
    *
    * @param realm the realm to join the session to
    * @return the (future of) session
    */
  def open(realm: Uri): CompletionStage[Session] = asJava {
    delegate.open(realm).map(s => new Session(delegate = s))
  }

  /**
    * Connects and then opens a new session joined to the given realm and
    * named endpoint
    *
    * @param endpoint is the name of a configured endpoint
    * @param realm the realm to join the session to
    * @return the (future of) session
    */
  def open(endpoint: String, realm: Uri): CompletionStage[Session] =asJava {
    delegate.open(endpoint, realm).map(s => new Session(delegate = s))
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
  def create(system: ActorSystem): Client = {
    new Client(ClientDelegate.apply(system))
  }
}