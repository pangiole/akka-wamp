package akka.wamp.client

import akka.actor._
import akka.io._
import akka.wamp._
import akka.wamp.messages._

import scala.concurrent._
import scala.concurrent.duration._

/**
  * INTERNAL API
  *
  * The connection actor which will keep (or break) the given promise of transport
  *
  * @param url is the URL to connect to (default is "ws://localhost:8080/ws")
  * @param subprotocol is the subprotocol to negotiate (default is "wamp.2.json")
  * @param promise is the promise of connection to fulfill
  */
private[client] 
class ClientActor(
  url: String, 
  subprotocol: String,
  maxAttempts: Int,
  promise: Promise[Transport]
) 
extends Actor
with ActorLogging
with ClientContext
{
  import ClientActor._
  
  var attempts = 0
  
  override def preStart(): Unit = {
    attempts = 0
    self ! DoConnect
  }
  
  override def receive: Receive = {
    case DoConnect =>
      val cmd = Connect(url, subprotocol)
      log.debug("=== {}", cmd)
      attempts = attempts + 1
      IO(Wamp) ! cmd

    case signal @ CommandFailed(cmd, ex) =>
      log.debug("=== {}", signal)
      if (attempts < maxAttempts) {
        scheduler.scheduleOnce(1 second, self, DoConnect)
      } else {
        promise.failure(new TransportException(ex.getMessage))
        context.stop(self)
      }
      
    case signal @ Connected(handler) =>
      log.debug("=== {}", signal)
      val transport = new Transport(self, handler)
      // switch its receive method so to delegate to the transport object
      context.become { 
        case msg => transport.receive(msg) 
      }
      promise.success(transport)
  }
}


private[client] object ClientActor {
  case object DoConnect
}