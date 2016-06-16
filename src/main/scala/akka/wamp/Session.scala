package akka.wamp

import akka.wamp.Wamp._
import akka.actor._
import scala.annotation.tailrec

/**
  * A Session is a transient conversation between two Peers (tipically a
  * a Router and a Client) attached to a Realm and running over 
  * a Transport.
  * 
  * Routing occurs only between Sessions that have joined the same Realm
  * 
  * @param id is the globally unique identifer
  * @param router is the Router actor reference
  * @param routerRoles are the Router's Roles
  * @param client is the Client actor reference
  * @param clientRoles are the Client's Roles
  * @param realm is a string identifying the Realm this session should attach to
  */
class Session(val id: Id, val router: ActorRef, val routerRoles: Set[String], val client: ActorRef, val clientRoles: Set[String], val realm: Uri)


object Session {
  /**
    * New session IDs in the global scope MUST be drawn randomly from a uniform
    * distribution over the complete range [ Id.MIN, Id.MAX ] and MUST
    * not be actually in use.
    *
    * @param used is the map of already used identifiers
    * @param id is the actual attempt (-1 by default)
    * @return the random identifier
    */
  @tailrec
  def generateId(used: Map[Id, Any], id: Id = -1): Id = {
    if (id == -1 || used.isDefinedAt(id)) generateId(used, Id.draw)
    else id
  }
}
