package akka.wamp.router

import akka.actor._
import akka.wamp._

import scala.annotation.tailrec

/**
  * A Session is a transient conversation between two peers (tipically a
  * a router and a client) joined to a realm and running over
  * a transport.
  * 
  * Routing occurs only between sessions that have joined the same realm
  * 
  * @param id is the session identifer in global scope
  * @param peer is the peer (tipically a client) actor reference
  * @param roles are the peer roles
  * @param realm is the realm this session joines to
  */
class Session(val id: Id, val peer: ActorRef, val roles: Set[String], val realm: Uri) extends akka.wamp.Session


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
