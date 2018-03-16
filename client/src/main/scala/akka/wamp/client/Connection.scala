package akka.wamp.client

import akka.actor._
import akka.wamp._
import akka.wamp.messages._

import scala.concurrent._
import java.net.URI

/**
  * Represents a connection established by a client to a router.
  *
  * Instances can be obtained by invoking the [[Client!.connect()*]] method.
  * {{{
  *   val client = ...
  *   val conn =
  *     client.connect("myrouter")
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
  * Once connected you can open a [[Session]].
  * {{{
  *   val session =
  *     conn.flatMap(c => c.open("myrealm"))
  * }}}
  *
  */
class Connection private[client](connector: ActorRef, addr: URI, fmt: String)(implicit private[client] val executionContext: ExecutionContext) extends akka.wamp.Connection {
  import ConnectionWorker._

  /* Is this connection disconnected */
  @volatile private[client] var disconnected = false

  /* Is this connection's session closed? */
  @volatile private[client] var session: Session = _

  /**
    * Is the address this client is connected to
    */
  val uri = addr

  /**
    * Is the format messages are encoded with
    */
  val format = fmt

  /**
    * Opens a session to be joined to the ``"default"`` realm
    */
  def open(): Future[Session] = open(Hello.defaultRealm)


  /**
    * Opens a session to be joined to the given realm.
    *
    * @param realm is the realm to join the session to
    * @return the (future of) session
    */
  def open(realm: Uri): Future[Session] = {
    withPromise[Session] { promise =>
      if (session == null || session.closed)
        connector ! SendHello(realm, promise)
      else
        promise.failure(new ClientException("Session already open", new IllegalStateException()))
    }
  }

  /**
    * Disconnects this connection
    *
    * @return the (future of) disconnected signal
    */
  def disconnect(): Future[Disconnected] = {
    withPromise[Disconnected] { promise =>
      connector ! SendDisconnect(promise)
    }
  }


  /* Create a promise and breaks it if this connection is not connected anymore */
  private def withPromise[T](fn: Promise[T] => Unit) = {
    val promise = Promise[T]
    if (disconnected)
      promise.failure(new ClientException("Disconnected", null))
    else
      fn(promise)
    promise.future
  }

}
