package akka.wamp

import akka.actor.ActorRef

import scala.annotation.tailrec

/**
  * A Session is a transient conversation between two [[Peer]]s (for example
  * a [[Client]] and a [[Router]]) attached to a [[Realm]] and running over 
  * a [[Transport]].
  * 
  * Routing occurs only between [[Session]]s that have joined the same [[Realm]]
  * 
  * @param id is the globally unique identifer
  * @param peer1
  * @param peer2
  */
class Session(var id: Long, var peer1: ActorRef, var peer2: ActorRef /* TODO realm: Uri*/)


object Session {
  /**
    * New session IDs in the global scope MUST be drawn randomly from a uniform
    * distribution over the complete range [ [[Id.MIN]], [[Id.MAX]] ] and MUST
    * not be actually in use.
    *
    * @param id is the actual attempt (-1 by default)
    * @param used are the actually used identifiers
    * @return the random identifier
    */
  @tailrec
  def randomIdNotIn(id: Long = -1)(used: Map[Long, _]): Long = {
    if (id == -1 || used.isDefinedAt(id)) randomIdNotIn(Id.draw)(used)
    else id
  }
}
