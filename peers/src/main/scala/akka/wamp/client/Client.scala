package akka.wamp.client

import akka.actor._
import akka.io.IO
import akka.wamp._
import akka.wamp.messages.Hello
import org.slf4j._

import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * WAMP clients are components which implement any or all of the 
  * subscriber, publisher, caller and callee roles. They can establish 
  * WAMP connections to a router and open new sessions.
  *
  * {{{
  *   import akka.wamp.client._
  *   val client = Client("myapp")
  *
  *   import scala.concurrent.Future
  *   implicit val ec = client.executionContext
  *
  *   val conn: Future[Connection] = client
  *     .connect(
  *       url = "ws://localhost:8080/router",
  *       subprotocol = "wamp.2.json"
  *     )
  * }}}
  * 
  * @param system is the Akka actor system
  */
class Client private[client]()(implicit system: ActorSystem) extends Peer {

  /**
    * This client actor system name
    */
  val name = system.name
  
  /**
    * The execution context of futures
    */
  implicit val executionContext = system.dispatcher 
  
  /** The logger */
  val log = system.log

  /**
    * Establish a WAMP connection to a router which is listening at 
    * the given URL and negotiate the given subprotocol
    * 
    * @param url is the URL to connect to (default is "ws://localhost:8080/router")
    * @param subprotocol is the subprotocol to negotiate (default is "wamp.2.json")
    * @param maxAttempts is the maximum number of attempts before giving up (default is 1)
    * @return the (future of) connection or [[TransportException]]
    */
  def connect(
    url: String = Client.defaultUrl, 
    subprotocol: String = Client.defaultSubprotocol,
    maxAttempts: Int = 1): Future[Transport] = 
  {
    val promise = Promise[Transport]
    system.actorOf(Props(new ClientActor(url, subprotocol, maxAttempts, promise)))
    promise.future
  }


  /**
    * Establish a WAMP connection to a router and open a new session
    *
    * @param url is the URL to connect to (default is "ws://localhost:8080/router")
    * @param subprotocol is the subprotocol to negotiate (default is "wamp.2.json")
    * @param realm is the realm to attach the session to (default is "default.realm")
    * @param roles is this client roles set (default is all possible client roles) 
    * @return the (future of) session or [[TransportException]] or [[AbortException]] 
    */
  def openSession(
    url: String = Client.defaultUrl, 
    subprotocol: String = Client.defaultSubprotocol, 
    realm: Uri = Hello.defaultRealm,
    roles: Set[Role] = Roles.client): Future[Session] = 
  {
    connect(url, subprotocol).flatMap(
      _.openSession(realm, roles)
    )
  }
  
  /**
    * Terminate this client.
    * 
    * {{{
    *   client.terminate().map { _ =>
    *     // ... after terminate completes ...
    *   }
    * }}}
    * 
    * @return the (future of) terminated event
    */
  def terminate(): Future[Terminated] = {
    system.terminate()
    // TODO https://github.com/angiolep/akka-wamp/issues/36
    // TODO Test what will happen on client terminate
  }
}

/**
  * Factory for Client instances.
  *
  * {{{
  *   import akka.wamp.client._
  *   val client = Client("myapp")
  *   
  *   import scala.concurrent.Future
  *   implicit val ec = client.executionContext
  *   
  *   val conn: Future[Connection] = client
  *     .connect(
  *       url = "ws://localhost:8080/router",
  *       subprotocol = "wamp.2.json",
  *       maxAttempts = 8
  *     )
  * }}}
  */
object Client {
  /**
    * Create a new client instance
    * 
    * @param name the unique name of the actor system
    * @return a new client instance
    */
  def apply(name: String = defaultName): Client = new Client()(ActorSystem(name))
  
  val defaultName = "default"
  
  val defaultUrl = "ws://localhost:8080/router"
  
  val defaultSubprotocol = "wamp.2.json"
}
