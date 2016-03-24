package akka.wamp

import akka.actor.{ActorRef, Props}
import akka.wamp.messages._

import scala.annotation.tailrec

/**
  * A Router is a [[Peer]] of the roles [[Broker]] and [[Dealer]] which is responsible 
  * for generic call and event routing and do not run any application code.
  * 
  * @param idgen is the session identifiers generator
  */
class Router (idgen: IdGenerator) extends Peer /* TODO with Broker*/ /* TODO with Dealer */ {
  import Router._
  
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
      sessionFor(peer2) match {
        case Some(s) =>
          /*
           * It is a protocol error to receive a second "HELLO" message 
           * during the lifetime of the session and the peer must fail 
           * the session if that happens.
           */
          sessions -= s.id
          peer2 ! ProtocolError("Session already open")   
          
        case None =>
          val id = idgen(sessions)
          sessions += (id -> new Session(id, self, peer2, realm))
          peer2 ! Welcome(id, Dict.withRoles("broker"))    
      }
      
    // TODO case Goodbye  
  }
  
  
  @tailrec
  private def newSessionId(id: Long = -1): Long = {
    if (id == -1 || sessions.contains(id)) newSessionId(Id.draw)
    else id
  }
  
  private def sessionFor(peer2: ActorRef) = {
    sessions.values.find(_.peer2 == peer2)
  }
}



object Router {
  /**
    * Create a Props for an actor of this type
    *
    * @param idgen is the identifier generator
    * @return the props
    */
  def props(idgen: IdGenerator = Session.randomIdNotIn()) = Props(new Router(idgen))
  
  /**
    * Sent when protocol errors occur
    * 
    * @param message
    */
  case class ProtocolError(message: String) extends Signal
  
}


