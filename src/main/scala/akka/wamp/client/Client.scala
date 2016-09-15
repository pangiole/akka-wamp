package akka.wamp.client

import akka.actor._
import akka.io.IO
import akka.wamp._
import akka.wamp.messages.Hello
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
  *   
  *   implicit val system = ActorSystem("myapp")
  *   val client = Client()
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
    * Client configuration 
    */
  private val config = system.settings.config.getConfig("akka.wamp.client")

  /**
    * Establish a WAMP connection to a router which is listening at 
    * the given URL and negotiate the given subprotocol
    * 
    * @param url is the URL to connect to
    * @param subprotocol is the subprotocol to negotiate
    * @return the (future of) connection or [[ConnectionException]]
    */
  def connect(
    url: String, 
    subprotocol: String = "wamp.2.json"): Future[Connection] = 
  {
    val promise = Promise[Connection]
    val client = system.actorOf(Props(new ConnectionActor(promise)))
    IO(Wamp) ! Wamp.Connect(client, url, subprotocol)
    promise.future
  }


  /**
    * Establish a WAMP connection to a router and open a new session
    *
    * @param url is the URL to connect to
    * @param subprotocol is the subprotocol to negotiate (default is "wamp.2.json")
    * @param realm is the realm to attach the session to (default is "akka.wamp.realm")
    * @param roles is this client roles set (default is all possible client roles) 
    * @return the (future of) session or [[ConnectionException]] or [[AbortException]] 
    */
  def connectAndOpenSession(
    url: String, 
    subprotocol: String = "wamp.2.json", 
    realm: Uri = Hello.defaultRealm,
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
  *   
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
