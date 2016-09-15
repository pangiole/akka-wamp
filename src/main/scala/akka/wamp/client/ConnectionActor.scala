package akka.wamp.client

import akka.actor.{Actor, ActorLogging}
import akka.wamp.{Validator, Wamp}

import scala.concurrent.Promise


/**
  * INTERNAL API
  *
  * The connection actor which will keep (or break) the given connection promise
  *
  * @param promise is the promise of connection to fulfill
  */
private[client] class ConnectionActor(promise: Promise[Connection]) extends Actor with ActorLogging {

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

  /**
    * This actor receive partial function
    */
  override def receive: Receive = {
    case signal @ Wamp.Connected(router) =>
      this.conn = new Connection(self, router)
      context.become {
        case message =>
          // delegate any "message processing" to the connection object
          conn.receive(message)
      }
      promise.success(conn)

    // TODO https://github.com/angiolep/akka-wamp/issues/29  
    // case command @ Wamp.Disconnect =>

    case signal @ Wamp.ConnectionFailed(cause) =>
      log.debug("!!! {}", signal)
      promise.failure(new ConnectionException(signal.toString))
      context.stop(self)

    case message =>
      log.debug("!!! {}", message)
      promise.failure(new ConnectionException(s"Unexpected message $message"))
      context.stop(self)
  }
}