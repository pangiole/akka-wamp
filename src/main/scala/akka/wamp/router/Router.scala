package akka.wamp.router


import akka.actor.{Scope => _, _}
import akka.io.IO
import akka.wamp.Wamp._
import akka.wamp._
import akka.wamp.client.Client
import akka.wamp.messages._

import scala.collection.mutable

/**
  * The router is a peer playing the broker and dealer which is 
  * responsible for generic call and event routing but do NOT run 
  * any application code a client would.
  * 
  */
final class Router(val scopes: Map[Symbol, Scope], val listener: Option[ActorRef]) 
  extends Peer with Broker with Dealer
  with Actor with ActorLogging  
{
  /** Router configuration */
  private val config = context.system.settings.config.getConfig("akka.wamp.router")

  /**
    * Boolean ifSession (default is false) to NOT automatically create
    * realms if they don't exist yet
    */
  val abortUnknownRealms = config.getBoolean("abort-unknown-realms")

  /**
    * Boolean ifSession (default is false) to validate against strict URIs
    * rather than loose URIs
    */
  val validateStrictUris = config.getBoolean("validate-strict-uris")

  /**
    * The boolean ifSession to disconnect those peers that 
    * send invalid messages.
    */
  val disconnectOffendingPeers = config.getBoolean("disconnect-offending-peers")

  
  
  /** WAMP types validator */
  protected implicit val validator = new Validator(validateStrictUris)
  
  /** Akka Execution Content */
  protected implicit val executionContext = context.system.dispatcher
  

  /** Details of WELCOME message replied by this router */
  private val welcomeDetails = Dict()
    .setAgent("akka-wamp-0.8.0")
    .addRoles(Roles.router)
  
  /**
    * Map of existing realms.
    *
    * A Realm is a routing and administrative __domain__, optionally
    * protected by authentication and authorization.
    *
    * Messages are only routed within a Realm.
    *
    */
  private[router] val realms = mutable.Set[Uri]("akka.wamp.realm")
  
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
    * Handle transports lifecycle signals and events such as: 
    * BOUND, CONNECTED, DISCONNECTED and UNBOUND
    */
  private def handleConnections: Receive = {
    case signal @ Wamp.Bound(url) =>
      log.info("[{}]    Successfully bound on {}", self.path.name, url)
      listener.map(_ ! signal)

    case signal @ Wamp.Connected(p) =>
      val peer = sender() // == p
      log.debug("[{}]     Wamp.Connected [{}]", self.path.name, peer.path.name)
      listener.map(_ ! signal)
      
    case signal @ Wamp.Disconnected =>
      val peer = sender()
      log.debug("[{}]     Wamp.Disconnected [{}]", self.path.name, peer.path.name)
      sessions.values.find(_.peer == peer) match {
        case Some(session) => closeSession(session)
        case None => ()
      }
      listener.map(_ ! signal)
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
          /*
           * It is a protocol error to receive a second "HELLO" message 
           * during the lifetime of the session and the peer must fail 
           * the session if that happens.
           */
          log.warning("[{}] !!! SessionException: received HELLO but session already open.", self.path.name)
          closeSession(session)
          if (disconnectOffendingPeers) {
            session.peer ! Wamp.Disconnect
          } else {
            session.peer ! Abort(reason = "akka.wamp.error.session_already_open")
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
      
    // ignore ABORT messages from transport
    case Abort => ()

    case msg @ Goodbye(details, reason) => {
      withSession(msg, sender(), role = None) { session =>
        closeSession(session)
        session.peer ! Goodbye(reason = "wamp.error.goodbye_and_out")
      }
    }
  }


  private[router] 
  def withSession(msg: Message, peer: ActorRef, role: Option[Role])(fn: (Session) => Unit): Unit = 
  {
    sessions.values.find(_.peer == peer) match {
      case Some(session) => {
        if (!role.isDefined) {
          fn(session)
        } 
        else {
          if (session.roles.contains(role.get)) {
            fn(session)
          } 
          else {
            if (disconnectOffendingPeers) {
              peer ! Wamp.Disconnect
            }
          }
        }
      }
      case None => {
        log.warning("SessionException: received message {} but NO session open.", msg)
        if (disconnectOffendingPeers) {
          peer ! Wamp.Disconnect
        }
      }
    }
  }

  private 
  def createNewSession(client: ActorRef, details: Dict, realm: Uri) = {
    val id = scopes('global).nextId(excludes = sessions.keySet.toSet)
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



object Router {

  /**
    * Create a Props for an actor of this type
    *
    * @param scopes is the [[Scope]] map used for [[Id]] generation
    * @param listener is the actor to notify Bind to
    * @return the props
    */
  def props(scopes: Map[Symbol, Scope] = Scope.defaults, listener: Option[ActorRef] = None) = 
    Props(new Router(scopes, listener))
  
  /**
    * Starts the router as standalone application
    * 
    * @param args
    */
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("wamp")
    system.actorOf(Props(new Binder()), name = "binder")
  }
  
  class Binder extends Actor with ActorLogging {
    implicit val system = context.system
    implicit val ec = context.system.dispatcher
    
    val router = context.system.actorOf(Router.props(), "router")
    IO(Wamp) ! Bind(router)
    
    def receive = {
      case Wamp.BindFailed(cause) =>
        context.system.terminate().map[Unit](_ => System.exit(-1))
    }
  }
}


