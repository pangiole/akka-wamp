package akka.wamp.client.japi

import java.util.concurrent.CompletionStage

import akka.wamp._
import akka.wamp.client.{Connection => ConnectionDelegate, Session => SessionDelegate}

import scala.compat.java8.FutureConverters._
import scala.concurrent.Future

/**
  * Represents a connection established by a client to a router.
  *
  * Instances can be obtained by invoking the [[Client!.connect()*]] method.
  * {{{
  *   Client client = ...
  *   CompletionStage<Connection> conn =
  *     client.connect("myrouter");
  * }}}
  *
  * Named transports can be configured as follows:
  * {{{
  *   akka.wamp.client.transport {
  *     myrouter {
  *       url = "wss://router.host.net:8443/wamp"
  *       format = "msgpack"
  *       min-backoff = 3 seconds
  *       max-backoff = 30 seconds
  *       random-factor = 0.2
  *     }
  *   }
  * }}}
  *
  * Once the connection is established you can open a new [[Session]].
  * {{{
  *   CompletionStage<Session> session =
  *     conn.theCompose(c -> c.open("myrealm"));
  * }}}
  *
  * @note This belongs to the Java API
  * @see [[akka.wamp.client.Connection]] for the Scala API
  */
class Connection(delegate: ConnectionDelegate) {
  
  private implicit val executionContext = delegate.executionContext

  /**
    * Is the address this client is connected to
    */
  val uri = delegate.uri

  /**
    * Is the format messages are encoded with
    */
  val format = delegate.format

  /**
    * Opens a session to be attached to the ``"default"``
    */
  def open(): CompletionStage[Session] =
    doOpen(delegate.open())

  /**
    * Opens a session to be attached to the given realm.
    *
    * @param realm is the realm to attach the session to
    * @return the (future of) session
    */
  def open(realm: Uri): CompletionStage[Session] =
    doOpen(delegate.open(realm))

  /* Does open */
  private def doOpen(fsd: => Future[SessionDelegate]): CompletionStage[Session] = toJava {
    for (sd <- fsd) yield new Session(delegate = sd)
  }

  /**
    * Disconnects this connection
    *
    * @return the (future of) disconnected signal
    */
  def disconnect(): CompletionStage[Disconnected] = toJava {
    delegate.disconnect().map(_ => new Disconnected)
  }
}
