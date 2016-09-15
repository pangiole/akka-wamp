package akka.wamp.client

import akka.actor.Actor.Receive
import akka.actor._
import akka.wamp._
import akka.wamp.messages._
import org.slf4j.LoggerFactory

import scala.concurrent.{Future, Promise}

/**
  * WAMP connections are established by clients to a router.
  * 
  * {{{
  *   import akka.actor._
  *   import akka.wamp.client._
  *
  *   implicit val system = ActorSystem("myapp")
  *   implicit val ec = system.dispatcher
  *
  *   val client = Client()
  *   val conn: Future[Connection] = client.connect(
  *     url = "ws://localhost:8080/router",
  *     subprotocol = "wamp.2.json"
  *   )
  *   
  * }}}
  *
  * 
  * WAMP connections can use any transport that is message-based, ordered,
  * reliable and bi-directional, with WebSocket as the default transport.
  * 
  * A WAMP session can be opened during the WAMP connection lifecycle, but
  * only one at the time.
  * 
  * {{{
  *    ,------.                                    ,------.
  *    | Peer |                                    | Peer |
  *    `--+---'                                    `--+---'
  *       |               TCP established             |
  *       |<----------------------------------------->|
  *       |                                           |
  *       |               TLS established             |
  *       |+<--------------------------------------->+|
  *       |+                                         +|
  *       |+           WebSocket established         +|
  *       |+|<------------------------------------->|+|
  *       |+|                                       |+|
  *       |+|            WAMP established           |+|
  *       |+|+<----------------------------------->+|+|
  *       |+|+                                     +|+|
  *       |+|+                                     +|+|
  *       |+|+            WAMP closed              +|+|
  *       |+|+<----------------------------------->+|+|
  *       |+|                                       |+|
  *       |+|                                       |+|
  *       |+|            WAMP established           |+|
  *       |+|+<----------------------------------->+|+|
  *       |+|+                                     +|+|
  *       |+|+                                     +|+|
  *       |+|+            WAMP closed              +|+|
  *       |+|+<----------------------------------->+|+|
  *       |+|                                       |+|
  *       |+|           WebSocket closed            |+|
  *       |+|<------------------------------------->|+|
  *       |+                                         +|
  *       |+              TLS closed                 +|
  *       |+<--------------------------------------->+|
  *       |                                           |
  *       |               TCP closed                  |
  *       |<----------------------------------------->|
  *       |                                           |
  *    ,--+---.                                    ,--+---.
  *    | Peer |                                    | Peer |
  *    `------'                                    `------'
  * }}}
  *
  * @param client is the client actor reference
  * @param router is the router representative actor reference
  * @param validator is the WAMP types validator
  */
class Connection private[client](client: ActorRef, router: ActorRef)(implicit validator: Validator) {
  private val log = LoggerFactory.getLogger(classOf[Connection])

  /**
    * The client actor reference
    */
  private[client] val clientRef: ActorRef = client

  /**
    * The router representative actor reference
    */
  private[client] val routerRef: ActorRef = router
  
  /**
    * Open a new session sending an HELLO message to the router 
    * for the given realm and roles
    * 
    * @param realm is the realm to attach the session to
    * @param roles is this client roles set
    * @return a (future of) session               
    */
  def openSession(realm: Uri = "akka.wamp.realm", roles: Set[Role] = Roles.client): Future[Session] = {
    val promise = Promise[Session]
    try {
      val hello = Hello(realm, Dict().withRoles(roles.toList: _*))
      become {
        handleWelcome(promise) orElse
        handleAbort(promise) orElse
        handleUnexpected(promise = Some(promise))
      }
      log.debug("--> {}", hello)
      routerRef ! hello
    } catch {
      case ex: Throwable =>
        log.debug(ex.getMessage)
        promise.failure(new ConnectionException(ex.getMessage))
    }
    promise.future
  }
  
  // TODO https://github.com/angiolep/akka-wamp/issues/29
  // def disconnect(): Future[Done]
  
  /**
    * Process any message received by the client
    */
  private[client] var receive: Receive =  _

  /**
    * Swap its receive function with the given one
    */
  private[client] def become(receive: Receive) = {
    this.receive = receive
  }

  /**
    * Send the given message to the router
    */
  private[client] def !(message: Message) = {
    log.debug("--> {}", message)
    // TODO routerRef.tell(message, Actor.noSender)
    routerRef ! message
  }

  /**
    * Handle an incoming WELCOME message 
    * by fulfilling the given promise of session
    */
  private def handleWelcome(promise: Promise[Session]): Receive = {
    case message: Welcome =>
      log.debug("<-- {}", message)
      val session = new Session(this, message)
      become {
        handleGoodbye(session) orElse
        handleUnexpected(None)
      }
      promise.success(session)
  }


  /**
    * Handle an incoming ABORT message 
    * by failing the given promise of session
    */
  private[client] def handleAbort(promise: Promise[Session]): Receive = {
    case message: Abort =>
      log.debug("<-- {}", message)
      promise.failure(new AbortException(message))
  }

  /**
    * Handle an incoming GOODBYE message
    * by sending a GOODBYE reply to the router
    * and closing the given session
    */
  private[client] def handleGoodbye(session: Session): Receive = {
    case message: Goodbye =>
      log.debug("<-- {}", message)
      /*
       * A session ends when the underlying transport disappears or 
       * when it is closed explicitly by a GOODBYE message sent by 
       * one peer and a GOODBYE message sent from the other peer 
       * in response.
       */
      routerRef ! Goodbye(Goodbye.defaultDetails, "wamp.error.goodbye_and_out")
      session.doClose()
  }


  /**
    * Handle any unexpected message by 
    * by failing the given (option of) promise of session
    */
  private[client] def handleUnexpected[T](promise: Option[Promise[T]]): Receive = {
    case message =>
      log.warn("!!! {}", message)
      promise.map { p =>
        if (!p.isCompleted)
          p.failure(new SessionException(s"unexpected $message"))
      }
  }
}


