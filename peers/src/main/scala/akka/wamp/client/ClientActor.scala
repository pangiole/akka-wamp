package akka.wamp.client

import akka.actor._
import akka.io._
import akka.wamp._
import akka.wamp.messages._

import scala.concurrent._


/**
  * INTERNAL API
  *
  * The connection actor which will keep (or break) the given promise of transport
  *
  * @param url is the URL to connect to (default is "ws://localhost:8080/router")
  * @param subprotocol is the subprotocol to negotiate (default is "wamp.2.json")
  * @param promise is the promise of connection to fulfill
  */
private[client] 
class ClientActor(url: String, subprotocol: String, promise: Promise[Transport]) extends Actor with ActorLogging {

  // TODO how about resiliency

  import context.system // implicitly used by IO(Wamp)
  IO(Wamp) ! Connect(url, subprotocol)
  
  /**
    * The connection
    */
  private var transport: Transport = _

  /**
    * Client configuration 
    */
  private val config = context.system.settings.config.getConfig("akka.wamp.client")

  /**
    * The boolean switch (default is false) to validate
    * against strict URIs rather than loose URIs
    */
  val strictUris = config.getBoolean("validate-strict-uris")

  /** WAMP types Validator */
  private implicit val validator = new Validator(strictUris)

  /** Akka actor system dispatcher */
  private implicit val executionContext: ExecutionContext = context.system.dispatcher

  /**
    * This actor receive partial function
    */
  override def receive: Receive = {
    case signal @ Connected(handler) =>
      log.debug("    {}", signal)
      // switch its receive method so to delegate to a connection object
      this.transport = new Transport(self, handler)
      context.become { case msg => transport.receive(msg) }
      promise.success(transport)
      
    case signal @ CommandFailed(_, ex) =>
      fail(signal, ex.getMessage)

    case signal @ Status.Failure(cause) =>
      fail(signal, cause.getMessage)

    case msg =>
      fail(msg, s"Unexpected message $msg")
  }
  
  private def fail(msg: Any, cause: String) = {
    log.warning("!!! {}", msg)
    promise.failure(new TransportException(cause))
    context.stop(self)
  }
}