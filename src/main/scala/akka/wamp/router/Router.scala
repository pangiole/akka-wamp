package akka.wamp.router

import akka.actor._
import akka.wamp._
import akka.wamp.Wamp._

import scala.annotation.tailrec
import scala.collection.mutable

/**
  * A Router is a Peer of the roles Broker and Dealer which is responsible 
  * for generic call and event routing and do not run any application code.
  * 
  */
class Router(nextSessionId: (Id) => Id) 
  extends Peer with Broker 
  with Actor with ActorLogging  
{
  
  val roles = Set("broker")
  
  val welcomeDetails = Dict()
    .withAgent(context.system.settings.config.getString("akka.wamp.agent"))
    .withRoles("broker")
  
  /**
    * Map of existing realms.
    *
    * A Realm is a routing and administrative __domain__, optionally
    * protected by authentication and authorization.
    *
    * Messages are only routed within a Realm.
    *
    */
  val realms = mutable.Set[Uri]("akka.wamp.realm")
  
  /**
    * Map of open Sessions
    */
  val sessions = mutable.Map.empty[Id, Session]
  

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.info("STARTING")
  }

  /**
    * Handle either sessions, subscriptions, publications, registrations or invocations
    */
  def receive = 
    handleSessions orElse 
      handleSubscriptions orElse
        handlePublications

  /**
    * Handle session lifecycle related messages such as: HELLO, WELCOME, ABORT and GOODBYE
    */
  private def handleSessions: Receive = {
    
    case msg @ Hello(realm, details) => 
      switchOn(client = sender()) (
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
            client ! Welcome(session.id, welcomeDetails)
          }
          else {
            /*
              * The behavior if a requested realm" does not presently exist 
              * is router-specific. A router may automatically create the realm, 
              * or deny the establishment of the session with a "ABORT" reply message. 
              */
            if (autoCreateRealms) {
              val session = newSession(client, details, createRealm(realm))
              client ! Welcome(session.id, welcomeDetails)
            }
            else {
              client ! Abort(Dict("message" -> s"The realm $realm does not exist."), "wamp.error.no_such_realm")
            } 
          }
        }
      )

      
    // ignore ABORT messages from client
    case msg: Abort =>
      ()  

      
    case msg @ Goodbye(details, reason) =>
      switchOn(client = sender()) (
        whenSessionOpen = { session =>
          closeSession(session)
          session.client ! Goodbye(Dict(), "wamp.error.goodbye_and_out")
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
    val id = nextId(sessions.keySet.toSet, nextSessionId)
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
  final def nextId(used: Set[Id], idGen: IdGenerator, id: Id = -1): Id = {
    if (id == -1 || used.contains(id)) nextId(used, idGen, idGen(id))
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
}


