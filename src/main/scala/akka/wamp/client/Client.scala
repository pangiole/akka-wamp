package akka.wamp.client

import akka.actor._
import akka.io.IO
import akka.wamp._
import org.slf4j._

import scala.concurrent.{Future, Promise}

/**
  * WAMP clients are components which implement any or all of the 
  * subscriber, publisher, caller and callee roles. They can establish 
  * WAMP connections to a router and open new sessions.
  * 
  * {{{
  *   import akka.actor._
  *   import akka.wamp.client._
  *   import akka.wamp.serialization._
  *   
  *   implicit val system = ActorSystem("myapp")
  *   implicit val ec = system.dispatcher
  *   
  *   val client = Client()
  *   val conn: Future[Connection] = client.connect("ws://host:9999/router")
  *   // ... map connection to ...
  * }}}
  * 
  * @param system the (implicit) actor system
  */
class Client private[client] ()(implicit system: ActorSystem) extends Peer {

  /**
    * The logger
    */
  private val log = LoggerFactory.getLogger(classOf[Client])

  /**
    * The execution context required by futures
    */
  private implicit val ec = system.dispatcher

  /**
    * Establish a WAMP connection to a router which is listening at 
    * the given URL and negotiate the given subprotocol
    * 
    * @param url is the URL to connect to
    * @param subprotocol is the subprotocol to negotiate
    * @return a (future of) connection
    */
  def connect(
    url: String = "ws://127.0.0.1:8080/ws", 
    subprotocol: String = "wamp.2.json"): Future[Connection] = 
  {
    val promise = Promise[Connection]
    val client = system.actorOf(Props(new ClientActor(promise)))
    IO(Wamp) ! Wamp.Connect(client, url, subprotocol)
    promise.future
  }


  /**
    * Establish a WAMP connection to a router and open a new session
    *
    * @param url is the URL to connect to
    * @param subprotocol is the subprotocol to negotiate
    * @param realm is the realm to attach the session to
    * @param roles is this client roles set
    * @return a (future of) session 
    */
  def connectAndOpenSession(
    url: String = "ws://localhost:8080/ws", 
    subprotocol: String = "wamp.2.json", 
    realm: Uri = "akka.wamp.realm",
    roles: Set[Role] = Roles.client): Future[Session] = 
  {
    for {
      conn <- connect(url, subprotocol)
      ssn <- conn.openSession(realm, roles)
    } yield ssn
  }
}

/**
  * Factory for [[Client]] instances.
  *
  * {{{
  *   import akka.actor._
  *   import akka.wamp.client._
  *   import akka.wamp.serialization._
  *
  *   implicit val system = ActorSystem("myapp")
  *   implicit val ec = system.dispatcher
  *
  *   val client = Client()
  *   val conn: Future[Connection] = client.connect("ws://host:9999/router")
  *   // ... map connection to ...
  * }}}
  */
object Client {
  /**
    * Create a new client instance
    * 
    * @param system is the (implicit) actor system
    * @return a new client instance
    */
  def apply()(implicit system: ActorSystem) = new Client()
}

/**
  * INTERNAL API
  * 
  * The client actor which will keep (or break) the given connection promise
  *  
  * @param promise is the promise of connection to fulfill
  */
private[client] class ClientActor(promise: Promise[Connection]) extends Actor with ActorLogging {

  /**
    * The connection
    */
  private var conn: Connection = _

  /**
    * The global configuration
    */
  private val config = context.system.settings.config

  /**
    * The boolean switch (default is false) to validate against strict URIs rather than loose URIs
    */
  private val strictUris = config.getBoolean("akka.wamp.serialization.validate-strict-uris")

  /**
    * The WAMP types Validator
    */
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