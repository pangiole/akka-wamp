package akka.wamp.router


import akka.actor.{Scope => _, _}
import akka.io.IO
import akka.wamp.Wamp._
import akka.wamp._
import akka.wamp.client.Client
import akka.wamp.messages._

import scala.collection.mutable

/**
  * The Router is a [[Peer]] playing the [[Roles.broker]] and [[Roles.dealer]] 
  * which is responsible for generic call and [[Event]] routing but do NOT run 
  * any application code a Client would.
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

  
  val defaultIfSessionIsClosed: (AbstractMessage, ActorRef) => Unit = { (msg, peer) =>
    log.warning("SessionException: received message {} but NO session open.", msg)
    if (disconnectOffendingPeers) peer ! Wamp.Disconnect
  }
  
  
  /** WAMP types validator */
  protected implicit val validator = new Validator(validateStrictUris)

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
        handleRegistrations //orElse handleCalls or Else handleYields

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
      ifSession(signal, sender())(
        isOpen = { session =>
          closeSession(session)
        }
      )
      listener.map(_ ! signal)
  }
  
  
  /**
    * Handle session lifecycle messages such as: 
    * HELLO, WELCOME, ABORT and GOODBYE
    */
  private def handleSessions: Receive = {
    case message @ Hello(realm, details) => {
      ifSession(message, sender())(
        isOpen = { session =>
          /*
           * It is a protocol error to receive a second "HELLO" message 
           * during the lifetime of the session and the peer must fail 
           * the session if that happens.
           */
          log.warning("[{}] !!! SessionException: received HELLO when session already open.", self.path.name)
          closeSession(session)
          if (!disconnectOffendingPeers) {
            session.peer ! Goodbye(Dict("message"->"Second HELLO message received during the lifetime of the session"), "akka.wamp.error.session_failure")
          } else {
            session.peer ! Wamp.Disconnect
          }
        },
        isClosed = { (_, peer) =>
          if (realms.contains(realm)) {
            val session = addNewSession(peer, details, realm)
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
              val session = addNewSession(peer, details, createRealm(realm))
              peer ! Welcome(session.id, welcomeDetails) 
            } 
          }
        }
      )
    }
      
    // ignore ABORT messages from transport
    case Abort => ()

    case message @ Goodbye(details, reason) => {
      ifSession(message, sender()) { session =>
        closeSession(session)
        session.peer ! Goodbye(Goodbye.defaultDetails, "wamp.error.goodbye_and_out")
        // DO NOT disconnect
      }
    }
  }


  private[router] 
  def ifSession(message: AbstractMessage, peer: ActorRef)
               (isOpen: (Session) => Unit, isClosed: (AbstractMessage, ActorRef) => Unit = defaultIfSessionIsClosed): Unit = {
    sessions.values.find(_.peer == peer) match {
      case Some(session) => isOpen(session)
      case None => isClosed(message, peer)
    }
  }

  private def addNewSession(client: ActorRef, details: Dict, realm: Uri) = {
    val id = scopes('global).nextId(excludes = sessions.keySet.toSet)
    val roles = details("roles").asInstanceOf[Map[String, Any]].keySet
    val session = new Session(id, client, roles, realm)
    sessions += (id -> session)
    session
  }
  
  private def closeSession(session: Session) = {
    subscriptions.foreach { case (_, subscription) => unsubscribe(session.peer, subscription) }
    // TODO unregister procedures on close session
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


