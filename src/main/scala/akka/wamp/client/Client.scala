package akka.wamp.client

import akka.actor.Status.{Failure => StreamFailure}
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.io.IO
import akka.stream.ActorMaterializer
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
  * @param materializer
  */
private[client] class Client()(implicit system: ActorSystem, materializer: ActorMaterializer) /*extends Peer*/ {
  private val log = LoggerFactory.getLogger(classOf[Client])
  private implicit val ec = system.dispatcher

  /**
    * It connects to a [[Router]] at the given URL negotiating the given subprotocol
    * 
    * @param url is the URL to connect to (default is "ws://localhost:8080/ws")
    * @param subprotocol is the subprotocol to negotiate (default is "wamp.2.json")
    * @return a future of [[Session]] that can be composed in monadic expressions
    */
  def connect(
    url: String = "ws://127.0.0.1:8080/ws", 
    subprotocol: String = "wamp.2.json"): Future[Transport] = 
  {
    // TODO retries: Int = 0
    val promise = Promise[Transport]
    val client = system.actorOf(Props(new ClientActor(promise)))
    IO(Wamp) ! Connect(client, url, subprotocol)
    promise.future
  }

  
  /**
    * It connects to a [[Router]] and says [[Hello]] to open a [[Session]]
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
    roles: Set[Role] = Set(Publisher, Subscriber)): Future[Session] = 
  {
    for {
      transport <- connect(url, subprotocol)
      session <- transport.open(realm, roles)
    } yield session
  }
}


object Client {
  def apply()(implicit system: ActorSystem, materializer: ActorMaterializer) = new Client()
}


// the actor which will keep (or break) the given promise
private[client] class ClientActor(promise: Promise[Transport]) extends Actor with ActorLogging {
  var transport: Transport = _
  
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
      
    /* case Wamp.Disconnected =>
      // TODO in which cases this message could be sent?
      log.debug("[{}] Disconnected from router [{}]", self.path.name, client.path.name)
      // router ! Wamp.Disconnect
      // client ! PoisonPill
      context.stop(self)
    */

    /*case StreamFailure(cause) =>
      // TODO in which cases this message could be sent?
      log.debug(cause.toString)
      promise.failure(cause)
      context.stop(self)*/  
  }
}