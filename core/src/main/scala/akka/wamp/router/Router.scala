package akka.wamp.router


import akka.actor.{Scope => _, _}
import akka.wamp._
import akka.wamp.messages._

import scala.collection.mutable

/**
  * The router is a peer playing the broker and dealer which is 
  * responsible for generic call and event routing but do NOT run 
  * any application code a client would.
  * 
  */
final class Router(val scopes: Map[Symbol, IdScope])
  extends Peer with Broker with Dealer
  with Actor with ActorLogging  
{
  import Router._

  /** Router configuration */
  private val config = context.system.settings.config.getConfig("akka.wamp.router")

  /**
    * Boolean ifSession (default is false) to NOT automatically create
    * realms if they don't exist yet
    */
  val abortUnknownRealms = config.getBoolean("abort-unknown-realms")

  /** IF drop an offending message and resume to the next one */
  private val dropOffendingMessages = config.getBoolean("drop-offending-messages")
  
  
  /** WAMP types validator */
  protected implicit val validator = new Validator(
    config.getBoolean("validate-strict-uris")
  )
  
  /** Akka Execution Content */
  protected implicit val executionContext = context.system.dispatcher
  

  /** Details of WELCOME message replied by this router */
  private val welcomeDetails = Dict()
    .withAgent("akka-wamp-0.13.0")
    .withRoles(Roles.router)
  
  /**
    * Map of existing realms.
    *
    * A Realm is a routing and administrative __domain__, optionally
    * protected by authentication and authorization.
    *
    * Messages are only routed within a Realm.
    *
    */
  private[router] val realms = mutable.Set[Uri]("default")
  
  /**
    * Map of open sessions
    */
  private[router] val sessions = mutable.Map.empty[Id, Session]

  /**
    * Handle either transports, sessions, subscriptions, publications, 
    * registrations or invocations
    */
  override def receive =
    handleConnections orElse handleSessions orElse 
      handleSubscriptions orElse handlePublications orElse
        handleRegistrations orElse handleCalls

  /**
    * Handle transports lifecycle signals such as DISCONNECTED
    */
  private def handleConnections: Receive = {
    case signal @ Connected(handler) =>
      log.debug("[{}]     Connected [{}]", self.path.name, handler.path.name)
      
    case signal @ Disconnected =>
      val handler = sender()
      log.debug("[{}]     Disconnected [{}]", self.path.name, handler.path.name)
      sessions.values.find(_.peer == handler) match {
        case Some(session) => closeSession(session)
        case None => ()
      }

    case cmd @ SimulateShutdown =>
      sessions.foreach { case (id, session) =>
        session.peer ! Goodbye(reason = "wamp.error.system_shutdown")
      }
  }


  /**
    * Handle session lifecycle messages such as:
    * HELLO, WELCOME, ABORT and GOODBYE
    */
  private def handleSessions: Receive = {
    case message @ Hello(realm, details) => {
      val peer = sender()
      sessions.values.find(_.peer == peer) match {
        case Some(session) => {
          if (!dropOffendingMessages) {
            /*
             * It is a protocol error to receive a second "HELLO" message
             * during the lifetime of the session and the peer must close
             * the session if that happens.
             */
            log.warning("[{}] !!! SessionException: received HELLO but session already open.", self.path.name)
            closeSession(session)
            session.peer ! Disconnect
          }
        }
        case None => {
          if (realms.contains(realm)) {
            val session = createNewSession(peer, details, realm)
            peer ! Welcome(session.id, welcomeDetails)
          }
          else {
            /*
              * The behavior if a requested realm" does not presently exist
              * is router-specific. A router may automatically create the realm,
              * or deny the establishment of the session with a "ABORT" reply message.
              */
            if (abortUnknownRealms) {
              peer ! Abort(Dict("message"->s"The realm '$realm' does not exist."), "wamp.error.no_such_realm")
            }
            else {
              val session = createNewSession(peer, details, createRealm(realm))
              peer ! Welcome(session.id, welcomeDetails)
            }
          }
        }
      }
    }

    case msg @ Goodbye(details, reason) => {
      withSession(msg, sender()) { session =>
        closeSession(session)
        session.peer ! Goodbye(reason = "wamp.error.goodbye_and_out")
      }
    }
  }


  private[router]
  def withSession(msg: Message, peer: ActorRef)(fn: (Session) => Unit): Unit =
  {
    sessions.values.find(_.peer == peer) match {
      case Some(session) => {
        fn(session)
      }
      case None => {
        log.warning("SessionException: received message {} but NO session open.", msg)
        if (!dropOffendingMessages) {
          peer ! Disconnect
        }
      }
    }
  }

  private
  def createNewSession(client: ActorRef, details: Dict, realm: Uri) = {
    val id = scopes('global).nextRequestId(excludes = sessions.keySet.toSet)
    val roles = details("roles").asInstanceOf[Map[String, Any]].keySet
    val session = new Session(id, client, roles, realm)
    sessions += (id -> session)
    session
  }

  private def closeSession(session: Session) = {
    subscriptions.foreach { case (_, subscription) => unsubscribe(session.peer, subscription) }
    registrations.foreach { case (_, registration) => unregister(session.peer, registration) }
    sessions -= session.id
  }

  private def createRealm(realm: Uri) = {
    realms += realm
    realm
  }
}


/**
  * Router companion object
  */
object Router {
  import IdScopes._

  private[wamp] case object SimulateShutdown

  /**
    * Create a Props for an actor of this type
    *
    * @param scopes is the [[IdScope]] map used for [[Id]] generation
    * @return the props
    */
  def props(scopes: Map[Symbol, IdScope] = Map(
    'global  -> new GlobalIdScope {},
    'router  -> new RouterIdScope {},
    'session -> new SessionIdScope{}
  ))
  = Props(new Router(scopes))
}


