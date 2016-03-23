package akka.wamp

import akka.actor.Props
import akka.wamp.messages._

import scala.annotation.tailrec

/**
  * A Router is a [[Peer]] of the roles [[Broker]] and [[Dealer]] which is responsible 
  * for generic call and event routing and do not run any application code.
  * 
  * @param idgen is the session identifiers generator
  */
class Router (idgen: IdGenerator) extends Peer /* TODO with Broker*/ /* TODO with Dealer */ {
  
  /**
    * Map of open [[Session]]s by their ids
    */
  var sessions = Map.empty[Long, Session]
  
  def receive = handleSessions /* TODO orElse handleSubscriptions*/ /* TODO orElse handleProcedures */

  /**
    * Handle session lifecycle related messages such as: HELLO, WELCOME, ABORT and GOODBYE
    */
  def handleSessions: Receive = {
    case Hello(realm, details) =>
      val peer2 = sender()
      val id = idgen(sessions)
      sessions += (id -> new Session(id, self, peer2, realm))
      peer2 ! Welcome(id, Map("roles" -> Map("broker" -> Map())))
      
    // TODO case Goodbye  
  }
  
  
  @tailrec
  private def newSessionId(id: Long = -1): Long = {
    if (id == -1 || sessions.contains(id)) newSessionId(Id.draw)
    else id
  }
}


object Router {
  def props(idgen: IdGenerator = Session.randomIdNotIn()) = 
    Props(new Router(idgen))
}

