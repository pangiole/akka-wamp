package akka.wamp.router

import akka.actor.Status.Failure
import akka.actor._
import akka.wamp.Wamp._


/**
  * A Transport connects two Peers and provides a channel over which
  * Messages for a Session can flow in both directions.
  *
  * @param router is the first Peer connected by this transport
  */
class WebSocketTransport(val router: ActorRef) extends Actor with ActorLogging {

  override def preStart(): Unit = {
    log.info("[transport/{}] - STARTING", self.path.name)
  }
  
  /**
    * It is the second Peer connected by this transport,
    */
  var client: ActorRef = _

  
  def receive: Receive = {
    case Connected(ref) =>
      client = ref
      log.debug("[transport/{}] - CONNECTED to client/{}", self.path.name, client.path.name)

    case msg: SpecifiedMessage =>
      if (client != null) router.tell(msg, client)
      // TODO else throw new IllegalStateException()

    case ConnectionClosed =>
      log.debug("[transport/{}] - DISCONNECTED from client/{}", self.path.name, client.path.name)
      context.stop(self)

    case Failure(ex) =>
      // TODO it should not disconnect on SerializingException but just drop the message
      log.warning("[transport/{}] - DISCONNECTED from client/{} due to {}: {}", self.path.name, client.path.name, ex.getClass.getName, ex.getMessage)
      client ! PoisonPill
      context.stop(self)
  }


  override def postStop(): Unit = {
    log.debug("[transport/{}] - STOPPED", self.path.name)
  }
}


object WebSocketTransport {
  /**
    * Create a Props for an actor of this type
    * 
    * @param router is the Router connected by the transport
    * @return the props
    */
  def props(router: ActorRef) = Props(new WebSocketTransport(router))
}
