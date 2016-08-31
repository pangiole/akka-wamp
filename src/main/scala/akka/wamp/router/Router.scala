package akka.wamp.router


import akka.actor.{Scope => _, _}
import akka.io.IO
import akka.wamp.Wamp._
import akka.wamp._
import akka.wamp.messages._

import scala.collection.mutable

/**
  * A Router is a Peer of the roles Broker and Dealer which is responsible 
  * for generic call and event routing and do not run any application code.
  * 
  */
final class Router(val scopes: Map[Symbol, Scope], val listener: Option[ActorRef]) 
  extends Peer with Broker 
  with Actor with ActorLogging  
{
  
  val config = context.system.settings.config
  
  val abortUnknownRealms = config.getBoolean(s"akka.wamp.router.abort-unknown-realms") 
  
  val strictUris = config.getBoolean(s"akka.wamp.serialization.validate-strict-uris")
  
  implicit val validator = new Validator(strictUris)
  
  val roles = Set("broker")
  
  val welcomeDetails = Dict()
    .withAgent("akka-wamp-0.6.0")
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

  /**
    * Handle either transports, sessions, subscriptions, publications, 
    * registrations or invocations
    */
  override def receive =
    handleTransports orElse
      handleSessions orElse 
        handleSubscriptions orElse
          handlePublications


  /**
    * Handle transports connection and disconnection
    */
  private def handleTransports: Receive = {
    case bound @ Bound(url) =>
      log.info("[{}] - Successfully bound on {}", self.path.name, url)
      listener.map(_ ! bound)
    case Disconnect =>
      val client = sender()
      log.info("[{}] - Client disconnected {}", self.path, client.path.name)
      switchOn(client)(
        whenSessionOpen = { session =>
          closeSession(session)
        },
        otherwise = { _ => () }
      )
  }
  
  /**
    * Handle session lifecycle related messages such as: HELLO, WELCOME, ABORT and GOODBYE
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
          log.warning("SessionException: received HELLO when session already open.")
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
      switchOn(sender())(
        whenSessionOpen = { session =>
          closeSession(session)
          session.client ! Goodbye(Goodbye.defaultDetails, "wamp.error.goodbye_and_out")
          // DO NOT disconnectTransport
        },
        otherwise = { transport =>
          // TODO Unspecified scenario. Ask for better WAMP protocol specification.
          log.warning("SessionException: received GOODBYE when no session")
        }
      )
    }
  }


  def switchOn(client: ActorRef)(whenSessionOpen: (Session) => Unit, otherwise: ActorRef => Unit): Unit = {
    sessions.values.find(_.client == client) match {
      case Some(session) => whenSessionOpen(session)
      case None => otherwise(client)
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
    subscriptions.foreach { case (_, subscription) => unsubscribe(session.client, subscription) }
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


