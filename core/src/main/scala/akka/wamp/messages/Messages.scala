package akka.wamp.messages

import java.net.URI

import akka.actor.ActorRef
import akka.http.scaladsl.Http
import akka.wamp._

/**
  * Represents all messages
  */
trait Message

/**
  * Represents command messages
  *
  */
trait Command extends Message

/**
  * This connect command is sent by a peer (a client) with the intent to establish
  * a transport to another peer (a router).
  *
  * @param address is the address to connect to (e.g. "ws://somehost.com:9999/path/to/router")
  * @param format is the format used to encode messages (e.g. "wamp.2.json")
  */
final case class Connect(address: URI, format: String) extends Command


/**
  * This handle command is sent by a connection listener upon receiving the incoming connection
  * signal to ask a newly spawned connection handler to handle it
  * 
  * @param conn is the incoming connection to be handled
  */
final case class HandleHttpConnection(conn: Http.IncomingConnection) extends Command

/**
  * This disconnect command is sent by client applications with the intent to connect from a router.
  */
final case object Disconnect extends Command


/**
  * Is the command to be sent to bind the given router actor
  * to all of its configured endpoints
  *
  * @param router is the router actor to bind
  * @param endpoint is the named configuration endpoint to bind to
  */
final case class Bind(router: ActorRef, endpoint: String) extends Command


/**
  * This unbind command is sent by routing applications with the intent to unbound from a transport.
  */
final case object Unbind


// ~~~~~~~~~~~~~~~~~~~~~~~~~~

/**
  * Common interface for all signals generated by this extension
  */
trait Signal extends Message

/**
  * This failure signal is replied back whenever a command fails
  *
  * @param cmd is the original command which failed
  * @param ex is the exception thrown
  */
case class CommandFailed(cmd: Command, ex: Throwable) extends Signal

/**
  * This bound signal is replied back whenever a [[Bind]] command succeeds.
  *
  * @param listener is the actor reference of the newly spawned connection listener actor
  * @param uri is the locator the connection listener is currently bound at
  */
final case class Bound(listener: ActorRef, uri: URI) extends Signal

// TODO final case object Unbound

/**
  * It the connected signal replied back whenever a [[Connect]] command succeed.
  *
  * @param handler is the actor reference of the newly spawned connection handler actor
  * @param uri is the URL to connect to (e.g. "ws://somehost.com:9999/path/to/router")
  * @param format is the format used to encode messages (e.g. "wamp.2.json")
  */
final case class Connected(handler: ActorRef, uri: URI, format: String) extends Signal

/**
  * This disconnected signal announces handler disconnection
  */
sealed abstract class Disconnected extends Signal
final case object Disconnected extends Disconnected


/**
  * This closed signal announces session closed
  */
sealed abstract class Closed extends Signal
final case object Closed extends Closed


/**
  * Factory of [[Message]] instances
  *
  */
object WampMessage {

  /**
    * Create a [[Bind]] command
    *
    * @param router is the router to bind
    * @param endpoint is the name of the configured endpoint to bind to
    */
  def bind(router: ActorRef, endpoint: String) = Bind(router, endpoint)

  /**
    * Create a [[Connect]] command
    *
    * @param uri is the address to connect to (e.g. "ws://somehost.com:9999/path/to/router")
    * @param format is the format used to encode messages (e.g. "wamp.2.json")
    */
  def connect(uri: URI, format: String) = Connect(uri, format)
}