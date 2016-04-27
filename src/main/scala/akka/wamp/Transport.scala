package akka.wamp

import akka.actor._


/**
  * A Transport connects two [[Peer]]s and provides a channel over which
  * [[Message]]s for a [[Session]] can flow in both directions.
  *
  * @param peer1 is the first [[Peer]] connected by this transport (for example a [[Router]])
  *              
  */
class Transport(val peer1: ActorRef) extends Actor with ActorLogging {
  import Transport._

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.info("START transport/{} for peer1/{}", self.path.name, peer1.path.name)
  }
  
  /**
    * It is the second [[Peer]] connected by this transport,
    * for example, if [[peer1]] is a [[Router]] then peer2 is a [[Client]]
    */
  var peer2: Option[ActorRef] = _

  def receive: Receive = {
    case Connect(ref) =>
      peer2 = Some(ref)
      log.debug("CONNECT peer1/{} to peer2/{} via transport/{}", peer1.path.name, peer2.get.path.name, self.path.name)

    case msg: Message =>
      if (peer2.isDefined) peer1.tell(msg, peer2.get)
      // TODO else throw new IllegalStateException()

    case Disconnect =>
      log.debug("DISCONNECT peer1/{} from peer2/{} via transport/{}", peer1.path.name, peer2.get.path.name, self.path.name)
      peer2 = None
      context.stop(self)

    case akka.actor.Status.Failure(ex) =>
      log.error(ex, ex.getMessage)
      //log.debug(s"FAILure -> count:$count, sender:${hash(sender)}, peer2:${hash(peer2)}, ex:${ex.getMessage}")
  }


  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    log.debug("STOP transport/{}", self.path.name)
  }

  private def hash(ref: ActorRef) = ref.hashCode()
}


object Transport {
  /**
    * Create a Props for an actor of this type
    * 
    * @param peer1 is the first [[Peer]] connected by the transport
    * @return the props
    */
  def props(peer1: ActorRef) = Props(new Transport(peer1))

  /**
    * Sent when the second [[Peer]] finally connects
    * 
    * @param ref is the actor reference of the second [[Peer]]
    */
  case class Connect(ref: ActorRef) extends Signal

  /**
    * Sent when the second [[Peer]] disconnects
    */
  case object Disconnect extends Signal
}
