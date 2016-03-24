package akka.wamp

import akka.actor.{Actor, ActorLogging, ActorRef, Props}


/**
  * A Transport connects two [[Peer]]s and provides a channel over which
  * [[Message]]s for a [[Session]] can flow in both directions.
  *
  * @param peer1 is the first [[Peer]] connected by this transport (for example a [[Router]])
  *              
  */
class Transport(peer1: ActorRef) extends Actor with ActorLogging {
  import Transport._
  
  // TODO could we get rid of count?
  private var count: Int = 0

  /**
    * It is the second [[Peer]] connected by this transport,
    * for example, if [[peer1]] is a [[Router]] then peer2 is a [[Client]]
    */
  var peer2: ActorRef = _

  def receive: Receive = {
    case Connect(ref) =>
      peer2 = ref
      count = count + 1
      log.debug(s"Connect -> count:$count, sender:${hash(sender)}, peer2:${hash(peer2)}")

    case msg: Message =>
      peer1.tell(msg, peer2)
      log.debug(s"Hello -> sender:${hash(sender)}, peer2:${hash(peer2)}")

    case Disconnect =>
      count = count - 1
      log.debug(s"DISconnect -> count:$count, sender:${hash(sender)}, peer2:${hash(peer2)}")

    case akka.actor.Status.Failure(ex) =>
      log.error(ex, ex.getMessage)
      // TODO count = count - 1
      //log.debug(s"FAILure -> count:$count, sender:${hash(sender)}, peer2:${hash(peer2)}, ex:${ex.getMessage}")
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
