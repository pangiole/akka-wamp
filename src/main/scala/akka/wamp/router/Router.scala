package akka.wamp.router

import akka.actor._
import akka.http.scaladsl.Http
import akka.io.IO
import akka.stream.ActorMaterializer
import akka.wamp.Wamp._
import akka.wamp._

import scala.annotation.tailrec
import scala.collection.mutable

/**
  * A Router is a Peer of the roles Broker and Dealer which is responsible 
  * for generic call and event routing and do not run any application code.
  * 
  */
private[wamp] class Router (nextSessionId: (Id) => Id)(implicit mat: ActorMaterializer) 
  extends Peer with Broker 
  with Actor with ActorLogging  
{

  val iface = context.system.settings.config.getString("akka.wamp.iface")
  val port = context.system.settings.config.getInt("akka.wamp.port")
  
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
    log.info("[{}] - Starting", self.path.name)
    import context.system
    IO(Wamp) ! Bind(self, iface, port)
  }


  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    import context.system
    IO(Wamp) ! Unbind
    log.info("[{}] - Stopped", self.path.name)
  }

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
              transport ! Abort(Dict("message" -> s"The realm $realm does not exist."), "wamp.error.no_such_realm")
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
          session.transport ! Goodbye(Dict(), "wamp.error.goodbye_and_out")
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
    val id = nextId(sessions.keySet.toSet, nextSessionId)
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
  def props(newSessionId: IdGenerator = (_) => Id.draw)(implicit mat: ActorMaterializer) = Props(new Router(newSessionId)(mat))


  /**
    * Starts the router as standalone application
    * 
    * @param args
    */
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("wamp")
    implicit val mat = ActorMaterializer()
    system.actorOf(Router.props(), "router")
  }
}


