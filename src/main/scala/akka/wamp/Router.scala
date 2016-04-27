package akka.wamp

import akka.actor._
import akka.wamp.Messages._

import scala.annotation.tailrec
import scala.collection.mutable

/**
  * A Router is a [[Peer]] of the roles [[Broker]] and [[Dealer]] which is responsible 
  * for generic call and event routing and do not run any application code.
  * 
  */
class Router(nextSessionId: (Id) => Id) extends Peer with Broker /* TODO with Dealer */ {
  
  import Router._

  /**
    * This router agent identification
    */
  val agent = context.system.settings.config.getString("akka.wamp.agent")
  
  /**
    * This router roles
    */
  val roles = Set("broker") // TODO Set("broker", "dealer")
  
  /**
    * Map of existing realms
    */
  val realms = mutable.Set[Uri]("akka.wamp.realm")
  
  /**
    * Map of open [[Session]]s
    */
  val sessions = mutable.Map.empty[Long, Session]

  /**
    * The JSON serializer (used for logging purposes only)
    */
  val ser = new JsonSerialization


  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.info("Starting peer1/{}", self.path.name)
  }

  /**
    * Handle either sessions, subscriptions, publications, registrations or invocations
    */
  def receive = 
    handleSessions orElse 
      handleSubscriptions orElse
        handlePublications
        /* TODO orElse handleProcedures */

  /**
    * Handle session lifecycle related messages such as: HELLO, WELCOME, ABORT and GOODBYE
    */
  private def handleSessions: Receive = {
    
    case msg @ Hello(realm, details) => 
      log.debug("HELLO{} from peer2/{}", ser.serialize(msg), sender.path.name)
      switchOn(sender) (
        whenSessionOpen = { session =>
          /*
           * It is a protocol error to receive a second "HELLO" message 
           * during the lifetime of the session and the peer must fail 
           * the session if that happens.
           */
          closeSession(session)
          session.client ! Failure("Session was already open.")
        },
        otherwise = { client =>
          if (realms.contains(realm)) {
            val session = newSession(client, details, realm)
            client ! Welcome(session.id, DictBuilder().withEntry("agent", agent).withRoles(roles).build())
          }
          else {
            /*
              * The behavior if a requested realm" does not presently exist 
              * is router-specific. A router may automatically create the realm, 
              * or deny the establishment of the session with a "ABORT" reply message. 
              */
            if (autoCreateRealms) {
              val session = newSession(client, details, createRealm(realm))
              client ! Welcome(session.id, DictBuilder().withEntry("agent", agent).withRoles(roles).build())
            }
            else {
              client ! Abort(DictBuilder().withEntry("message", s"The realm $realm does not exist.").build(), "wamp.error.no_such_realm")
            } 
          }
        }
      )

      
    // ignore ABORT messages from client
    case msg: Abort =>
      log.debug("ABORT{} from peer2/{}", ser.serialize(msg), sender.path.name)
      ()  

      
    case msg @ Goodbye(details, reason) =>
      log.debug("GOODBYE{} from peer2/{}", ser.serialize(msg), sender.path.name)
      switchOn(sender)(
        whenSessionOpen = { session =>
          closeSession(session)
          session.client ! Goodbye(DictBuilder().build(), "wamp.error.goodbye_and_out")
        },
        otherwise = { client =>
          client ! Failure("Session was not open yet.")
        }
      )
      
  }


  def switchOn(client: ActorRef)(whenSessionOpen: (Session) => Unit, otherwise: ActorRef => Unit): Unit = {
    sessions.values.find(_.client == client) match {
      case Some(session) => whenSessionOpen(session)
      case None => otherwise(client)
    }
  }
  
  private def newSession(client: ActorRef, details: Dict, realm: Uri) = {
    val id = nextId(sessions.toMap, nextSessionId)
    val session = new Session(id, router = self, routerRoles = roles, client, clientRoles = details("roles").asInstanceOf[Map[String, Any]].keySet, realm)
    sessions += (id -> session)
    session
  }
  
  private def closeSession(session: Session) = {
    subscriptions.foreach { case (_, subscription) => unsubscribe(session.client, subscription) }
    // TODO remove client from registrations
    sessions -= session.id
  }
  
  private def createRealm(realm: Uri) = {
    realms += realm
    realm
  }
  
  private val autoCreateRealms = context.system.settings.config.getBoolean("akka.wamp.auto-create-realms")

  @tailrec
  final def nextId(used: Map[Id, Any], idGen: IdGenerator, id: Id = -1): Id = {
    if (id == -1 || used.isDefinedAt(id)) nextId(used, idGen, idGen(id))
    else id
  }
}



object Router {
  
  /**
    * Create a Props for an actor of this type
    *
    * @param newSessionId is the session IDs generator
    * @return the props
    */
  def props(newSessionId: IdGenerator = (_) => Id.draw) = Props(new Router(newSessionId))
  
  
  /**
    * Sent when protocol errors occur and session must be failed
    * 
    * @param message
    */
  case class Failure(message: String) extends Signal
}


