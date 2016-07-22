package akka.wamp.router


import akka.actor.{Scope => _, _}
import akka.http.scaladsl.Http
import akka.io.IO
import akka.stream.ActorMaterializer
import akka.wamp.Wamp._
import akka.wamp._
import akka.wamp.messages._

import scala.collection.mutable

/**
  * A Router is a Peer of the roles Broker and Dealer which is responsible 
  * for generic call and event routing and do not run any application code.
  * 
  */
final class Router(val scopes: Map[Symbol, Scope], val listener: Option[ActorRef])(implicit mat: ActorMaterializer) 
  extends Peer with Broker 
  with Actor with ActorLogging  
{
  val autoCreateRealms = context.system.settings.config.getBoolean("akka.wamp.router.auto-create-realms")
  
  val roles = Set("broker")
  
  val welcomeDetails = Dict()
    .withAgent(context.system.settings.config.getString("akka.wamp.router.agent"))
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
  def receive =
    handleTransports orElse
      handleSessions orElse 
        handleSubscriptions orElse
          handlePublications

  /**
    * Handle transports connection and disconnection
    */
  private def handleTransports: Receive = {
    case msg @ Wamp.Bound(localAddress) =>
      log.info("[{}] - Successfully bound on {}", self.path.name, localAddress)
      for (lstnr <- listener) lstnr ! msg
      
    case conn: Http.IncomingConnection =>
      val transport = context.actorOf(Transport.props(self)(mat))
      transport ! conn
      
    case Wamp.Disconnect => 
      switchOn(sender())(
        whenSessionOpen = { session =>
          closeSession(session)
        },
        otherwise = { transport =>
          ()
        }
      )
  }
  
  /**
    * Handle session lifecycle related messages such as: HELLO, WELCOME, ABORT and GOODBYE
    */
  private def handleSessions: Receive = {
    case Hello(realm, details) => {
      switchOn(transport = sender())(
        whenSessionOpen = { session =>
          /*
           * It is a protocol error to receive a second "HELLO" message 
           * during the lifetime of the session and the peer must fail 
           * the session if that happens.
           */
          closeSession(session)
          session.transport ! Failure("Session was already open.")
        },
        otherwise = { transport =>
          if (realms.contains(realm)) {
            val session = addNewSession(transport, details, realm)
            transport ! Welcome(session.id, welcomeDetails)
          }
          else {
            /*
              * The behavior if a requested realm" does not presently exist 
              * is router-specific. A router may automatically create the realm, 
              * or deny the establishment of the session with a "ABORT" reply message. 
              */
            if (autoCreateRealms) {
              val session = addNewSession(transport, details, createRealm(realm))
              transport ! Welcome(session.id, welcomeDetails)
            }
            else {
              transport ! Abort("wamp.error.no_such_realm", Dict("message" -> s"The realm $realm does not exist."))
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
          session.transport ! Goodbye("wamp.error.goodbye_and_out", Dict())
          // DO NOT disconnectTransport
        },
        otherwise = { transport =>
          transport ! Failure("Session was not open yet.")
          // DO NOT disconnectTransport
        }
      )
    }
  }


  def switchOn(transport: ActorRef)(whenSessionOpen: (Session) => Unit, otherwise: ActorRef => Unit): Unit = {
    sessions.values.find(_.transport == transport) match {
      case Some(session) => whenSessionOpen(session)
      case None => otherwise(transport)
    }
  }
  
  private def addNewSession(transport: ActorRef, details: Dict, realm: Uri) = {
    val id = scopes('global).nextId(excludes = sessions.keySet.toSet)
    val roles = details("roles").asInstanceOf[Map[String, Any]].keySet
    val session = new Session(id, transport, roles, realm)
    sessions += (id -> session)
    session
  }
  
  private def closeSession(session: Session) = {
    subscriptions.foreach { case (_, subscription) => unsubscribe(session.transport, subscription) }
    // TODO remove transport from registrations
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
    * @param listener is the actor to notify router signals
    * @return the props
    */
  def props(scopes: Map[Symbol, Scope] = Scope.defaults, listener: Option[ActorRef] = None)(implicit mat: ActorMaterializer) = 
    Props(new Router(scopes, listener)(mat))


  /**
    * Starts the router as standalone application
    * 
    * @param args
    */
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("wamp")
    implicit val mat = ActorMaterializer()

    val router = system.actorOf(Router.props(), "router")
    IO(Wamp) ! Bind(router)
  }
}


