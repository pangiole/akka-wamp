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
  * any application code a [[Client]] would.
  * 
  */
final class Router(val scopes: Map[Symbol, Scope], val listener: Option[ActorRef]) 
  extends Peer with Broker with Dealer
  with Actor with ActorLogging  
{
  /**
    * Global configuration 
    */
  private[router] val config = context.system.settings.config

  /**
    * Boolean switch (default is false) to NOT automatically create
    * realms if they don't exist yet
    */
  private[router] val abortUnknownRealms = config.getBoolean("akka.wamp.router.abort-unknown-realms")

  /**
    * Boolean switch (default is false) to validate against strict URIs
    * rather than loose URIs
    */
  private[router] val strictUris = config.getBoolean("akka.wamp.serialization.validate-strict-uris")

  /**
    * Validator that validates values against WAMP protocol types
    */
  private[router] implicit val validator = new Validator(strictUris)

  /**
    * Details of WELCOME message replied by this router
    */
  private[router] val welcomeDetails = Dict()
    .withAgent("akka-wamp-0.7.0")
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
    handleTransports orElse handleSessions orElse 
      handleSubscriptions orElse handlePublications orElse
        handleRegistrations

  /**
    * Handle transports lifecycle signals and events such as: 
    * BOUND, CONNECT, DISCONNECT and UNBOUND
    */
  private def handleTransports: Receive = {
    case bound @ Bound(url) =>
      log.info("[{}]    Successfully bound on {}", self.path.name, url)
      listener.map(_ ! bound)
    
    // TODO case Connect  
    
    case Disconnect =>
      val client = sender()
      log.debug("[{}]    Client disconnected {}", self.path.name, client.path.name)
      switchOn(client)(
        whenSessionOpen = { session =>
          closeSession(session)
        },
        otherwise = { _ => () }
      )
  }
  
  
  /**
    * Handle session lifecycle messages such as: 
    * HELLO, WELCOME, ABORT and GOODBYE
    */
  private def handleSessions: Receive = {
    case Hello(realm, details) => {
      switchOn(client = sender())(
        whenSessionOpen = { session =>
          /*
           * It is a protocol error to receive a second "HELLO" message 
           * during the lifetime of the session and the peer must fail 
           * the session if that happens.
           */
          // TODO Unspecified scenario. Ask for better WAMP protocol specification.
          log.warning("[{}] !!! SessionException: received HELLO when session already open.", self.path.name)
          closeSession(session)
        },
        otherwise = { client =>
          if (realms.contains(realm)) {
            val session = addNewSession(client, details, realm)
            client ! Welcome(session.id, welcomeDetails)
          }
          else {
            /*
              * The behavior if a requested realm" does not presently exist 
              * is router-specific. A router may automatically create the realm, 
              * or deny the establishment of the session with a "ABORT" reply message. 
              */
            if (abortUnknownRealms) {
              client ! Abort(Dict("message" -> s"The realm $realm does not exist."), "wamp.error.no_such_realm")
            }
            else {
              val session = addNewSession(client, details, createRealm(realm))
              client ! Welcome(session.id, welcomeDetails) 
            } 
          }
        }
      )
    }
      
    // ignore ABORT messages from transport
    case Abort => ()

    case Goodbye(details, reason) => {
      switchOn(client = sender())(
        whenSessionOpen = { session =>
          closeSession(session)
          session.client ! Goodbye(Goodbye.defaultDetails, "wamp.error.goodbye_and_out")
          // DO NOT disconnectTransport
        },
        otherwise = { _ =>
          // TODO Unspecified scenario. Ask for better WAMP protocol specification.
          log.warning("[{}] !!! SessionException: received GOODBYE when no session", self.path.name)
        }
      )
    }
  }


  private[router] def switchOn(client: ActorRef)(whenSessionOpen: (Session) => Unit, otherwise: ActorRef => Unit): Unit = {
    sessions.values.find(_.client == client) match {
      case Some(session) => whenSessionOpen(session)
      case None => otherwise(client)
    }
  }

  private[router] def ifSessionOpen(message: Message)(fn: (Session) => Unit): Unit = {
    switchOn(sender())(
      whenSessionOpen = { session =>
        fn(session)
      },
      otherwise = { _ =>
        // TODO Unspecified scenario. Ask for better WAMP protocol specification.
        log.warning("SessionException: received {} when no session open yet.", message)
      }
    )
  }
  
  private def addNewSession(client: ActorRef, details: Dict, realm: Uri) = {
    val id = scopes('global).nextId(excludes = sessions.keySet.toSet)
    val roles = details("roles").asInstanceOf[Map[String, Any]].keySet
    val session = new Session(id, client, roles, realm)
    sessions += (id -> session)
    session
  }
  
  private def closeSession(session: Session) = {
    subscriptions.foreach { case (_, subscription) => unsubscribe(session.client, subscription) }
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


