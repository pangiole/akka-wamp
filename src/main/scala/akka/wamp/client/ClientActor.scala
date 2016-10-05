package akka.wamp.client

import akka.actor.{Actor, ActorLogging, Status}
import akka.io.IO
import akka.wamp.{Validator, Wamp}

import scala.concurrent.{ExecutionContext, Promise}


/**
  * INTERNAL API
  *
  * The connection actor which will keep (or break) the given connection promise
  *
  * @param url is the URL to connect to (default is "ws://localhost:8080/router")
  * @param subprotocol is the subprotocol to negotiate (default is "wamp.2.json")
  * @param promise is the promise of connection to fulfill
  */
private[client] 
class ClientActor(url: String, subprotocol: String, promise: Promise[Connection]) extends Actor with ActorLogging {

  // TODO how about resiliency

  import context.system // implicitly used by IO(Wamp)
  IO(Wamp) ! Wamp.Connect(url, subprotocol)
  
  /**
    * The connection
    */
  private var conn: Connection = _

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
    case signal @ Wamp.Connected(router) =>
      log.debug("    {}", signal)
      // switch its receive method so to delegate to a connection object
      this.conn = new Connection(self, router)
      context.become { case msg => conn.receive(msg) }
      promise.success(conn)

    // TODO https://github.com/angiolep/akka-wamp/issues/29  
    // case command @ Wamp.Disconnect =>
      
    case signal @ Wamp.CommandFailed(cmd, ex) =>
      fail(signal.toString, ex.getMessage)

    case signal @ Status.Failure(cause) =>
      fail(signal.toString, cause.getMessage)

    case msg =>
      log.warning("!!! {}", msg)
      promise.failure(new ConnectionException(s"Unexpected message $msg"))
      context.stop(self)
  }
  
  private def fail(signal: String, cause: String) = {
    log.warning("!!! {}", signal)
    promise.failure(new ConnectionException(cause))
    context.stop(self)
  }
}