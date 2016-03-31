package akka.wamp

import akka.actor.{ActorRef, Props}
import akka.wamp.messages._



/**
  * A Router is a [[Peer]] of the roles [[Broker]] and [[Dealer]] which is responsible 
  * for generic call and event routing and do not run any application code.
  * 
  * @param generateSessionId is the session IDs generator
  * @param generateSubscriptionId is the subscription IDs generator
  */
class Router (val generateSessionId: IdGenerator, val generateSubscriptionId: IdGenerator) 
  extends Peer with Broker /* TODO with Dealer */ {
  
  import Router._

  /**
    * This router roles
    */
  val roles = Set("broker") // TODO Set("broker", "dealer")
  
  /**
    * Map of existing realms
    */
  var realms = Set[Uri]("akka.wamp.realm")
  
  /**
    * Map of open [[Session]]s by their ids
    */
  var sessions = Map.empty[Long, Session]

  /**
    * Handle either sessions, subscriptions or procedure
    */
  def receive = handleSessions orElse handleSubscriptions /* TODO orElse handleProcedures */

  /**
    * Handle session lifecycle related messages such as: HELLO, WELCOME, ABORT and GOODBYE
    */
  def handleSessions: Receive = {
    
    case Hello(realm, details) => 
      findSessionBy(sender()) (
        whenFound = (session) => {
          /*
           * It is a protocol error to receive a second "HELLO" message 
           * during the lifetime of the session and the peer must fail 
           * the session if that happens.
           */
          closeSession(session.id)
          session.clientRef ! ProtocolError("Session was already open.")
        },
        otherwise = (clientRef) => {
          if (realms.contains(realm)) {
            openSession(clientRef, details, realm)
          }
          else {
            /*
              * The behavior if a requested "Realm" does not presently exist 
              * is router-specific. A router may automatically create the realm, 
              * or deny the establishment of the session with a "ABORT" reply message. 
              */
            if (autoCreateRealms) {
              createRealm(realm)
              openSession(clientRef, details, realm)
            }
            else {
              clientRef ! Abort(DictBuilder().withEntry("message", s"The realm $realm does not exist.").build(), "wamp.error.no_such_realm")
            } 
          }
        }
      )

      
    // ignore ABORT messages from client
    case Abort => ()  

      
    case Goodbye(details, reason) =>
      findSessionBy(sender())(
        whenFound = (session) => {
          closeSession(session.id)
          session.clientRef ! Goodbye(DictBuilder().build(), "wamp.error.goodbye_and_out")
        },
        otherwise = (clientRef) => {
          clientRef ! ProtocolError("Session was not open yet.")
        }
      )
      
  }

  
  def findSessionBy(clientRef: ActorRef)(whenFound: (Session) => Unit, otherwise: (ActorRef) => Unit): Unit = {
    sessions.values.find(_.clientRef == clientRef) match {
      case Some(session) => whenFound(session)
      case None => otherwise(clientRef)
    }
  }
  
  private def openSession(clientRef: ActorRef, details: Dict, realm: Uri): Unit = {
    val id = generateSessionId(sessions, -1)
    sessions += (id -> new Session(id, routerRef = self, routerRoles = roles, clientRef, clientRoles = details("roles").asInstanceOf[Map[String, Any]].keySet, realm))
    log.debug(s"Open session $id")
    // TODO how to read the artifact version from build.sbt?
    clientRef ! Welcome(id, DictBuilder().withEntry("agent", "akka-wamp-0.1.0").withRoles(roles).build())
  }
  
  private def closeSession(id: Long) = {
    log.debug(s"Close session $id")
    sessions -= id
  }
  
  private def createRealm(realm: Uri) = {
    log.debug(s"Create realm $realm")
    realms += realm
  }
  
  private val autoCreateRealms = context.system.settings.config.getBoolean("akka.wamp.auto-create-realms")
}



object Router {
  /**
    * Create a Props for an actor of this type
    *
    * @param generateSessionId is the session IDs generator
    * @param generateSubscriptionId is the subscription IDs generator
    * @return the props
    */
  def props(generateSessionId: IdGenerator = Session.generateId, generateSubscriptionId: IdGenerator = Subscription.generateId) = Props(new Router(generateSessionId, generateSubscriptionId))
  
  
  /**
    * Sent when protocol errors occur
    * 
    * @param message
    */
  case class ProtocolError(message: String) extends Signal
  
}


