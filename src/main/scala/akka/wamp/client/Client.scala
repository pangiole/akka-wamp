package akka.wamp.client

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.io.IO
import akka.wamp.Roles._
import akka.wamp.Wamp._
import akka.wamp._
import akka.wamp.messages._
import org.slf4j.LoggerFactory

import scala.concurrent.{Future, Promise}

/**
  * It is the entry point of the Akka Wamp future-based API
  * 
  * @param system
  */
private[client] class Client()(implicit system: ActorSystem) /*extends Peer*/ {
  private val log = LoggerFactory.getLogger(classOf[Client])
  private implicit val ec = system.dispatcher

  /**
    * It connects to a router at the given URL negotiating the given subprotocol
    * 
    * @param url is the URL to connect to (default is "ws://localhost:8080/ws")
    * @param subprotocol is the subprotocol to negotiate (default is "wamp.2.json")
    * @return a future of [[Session]] that can be composed in monadic expressions
    */
  def connect(
    url: String = "ws://127.0.0.1:8080/ws", 
    subprotocol: String = "wamp.2.json"): Future[Transport] = 
  {
    val promise = Promise[Transport]
    val client = system.actorOf(Props(new ClientActor(promise)))
    IO(Wamp) ! Connect(client, url, subprotocol)
    promise.future
  }

  
  /**
    * It connects to a router and says [[Hello]] to open a [[Session]]
    * 
    * @param url
    * @param subprotocol
    * @param realm
    * @param roles
    * @return
    */
  def connectAndOpen(
    url: String = "ws://localhost:8080/ws", 
    subprotocol: String = "wamp.2.json", 
    realm: Uri = "akka.wamp.realm",
    roles: Set[Role] = Set(publisher, subscriber)): Future[Session] = 
  {
    for {
      transport <- connect(url, subprotocol)
      session <- transport.open(realm, roles)
    } yield session
  }
}


object Client {
  def apply()(implicit system: ActorSystem) = new Client()
}


// the actor which will keep (or break) the given promise
private[client] class ClientActor(promise: Promise[Transport]) extends Actor with ActorLogging {
  var transport: Transport = _

  val config = context.system.settings.config
  val strictUris = config.getBoolean("akka.wamp.serialization.validate-strict-uris")
  implicit val validator = new Validator(strictUris)
  
  def receive = {
    case Connected(router) =>
      this.transport = new Transport(self, router)
      context.become { 
        case message => transport.receive(message) 
      }
      promise.success(transport)

    case message: ConnectionFailed =>
      log.debug(message.toString)
      promise.failure(new ConnectionException(message.toString))
      context.stop(self)
  }
}