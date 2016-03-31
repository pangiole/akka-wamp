package akka.wamp

import akka.actor.ActorRef

import scala.annotation.tailrec

/**
  * A Session is a transient conversation between two [[Peer]]s (tipically a
  * a [[Router]] and a [[Client]]) attached to a [[Realm]] and running over 
  * a [[Transport]].
  * 
  * Routing occurs only between [[Session]]s that have joined the same [[Realm]]
  * 
  * @param id is the globally unique identifer
  * @param routerRef is the [[Router]] actor reference
  * @param routerRoles are the [[Router]]'s [[Role]]s
  * @param clientRef is the [[Client]] actor reference
  * @param clientRoles are the [[Client]]'s [[Role]]s
  * @param realm is a string identifying the [[Realm]] this session should attach to
  */
class Session(val id: Id, val routerRef: ActorRef, val routerRoles: Set[String], val clientRef: ActorRef, val clientRoles: Set[String], val realm: Uri)


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
  def generateId(used: Map[Id, Any], id: Id = -1): Id = {
    if (id == -1 || used.isDefinedAt(id)) generateId(used, Id.draw)
    else id
  }
}
