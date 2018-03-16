package akka.wamp.router

import akka.actor._
import akka.wamp._
import akka.wamp.messages._

import scala.collection.mutable

/**
  * Represents a router.
  *
  * Instances can be created using its companion object.
  *
  */
class Router private[wamp](val idGenerators: Map[Symbol, IdGenerator])
  extends Peer with Broker with Dealer
  with Actor with ActorLogging
{
  import Router._

  /** Router configuration */
  private val config = context.system.settings.config.getConfig("akka.wamp.router")

  /**
    * The boolean switch to abort upon receiving HELLO for unknown realms.
    */
  val abortUnknownRealms = config.getBoolean("abort-unknown-realms")

  /**
    * The boolean switch to tolerate protocol violations.
    */
  private val tolerateProtocolViolations = config.getBoolean("tolerate-protocol-violations")


  /** WAMP types validator */
  protected implicit val validator = new Validator(
    config.getBoolean("validate-strict-uris")
  )

  /** Akka Execution Content */
  protected implicit val executionContext = context.system.dispatcher


  /** Details of WELCOME message replied by this router */
  private val welcomeDetails = Dict()
    .withAgent("akka-wamp-0.15.2")
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
    case signal @ Connected(handler, _, _) =>
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
          if (!tolerateProtocolViolations) {
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
  def withSession(msg: ProtocolMessage, peer: ActorRef)(fn: (Session) => Unit): Unit =
  {
    sessions.values.find(_.peer == peer) match {
      case Some(session) => {
        fn(session)
      }
      case None => {
        log.warning("SessionException: received message {} but NO session open.", msg)
        if (!tolerateProtocolViolations) {
          peer ! Disconnect
        }
      }
    }
  }

  private
  def createNewSession(client: ActorRef, details: Dict, realm: Uri) = {
    val id = idGenerators('global).nextId(excludes = sessions.keySet.toSet)
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
  * Factory of [[Router]]'s props instances
  */
object Router {

  private[wamp] case object SimulateShutdown

  /**
    * Create a Props for an actor of this type
    *
    * @return the props
    */
  def props() = Props(new Router(idGenerators = Map(
    'global  -> new GlobalScopedIdGenerator,
    'router  -> new RouterScopedIdGenerator,
    'session -> new SessionScopedIdGenerator
  )))
}


