package akka.wamp.client

import akka.actor.Actor._
import akka.wamp._
import akka.wamp.messages._

import scala.collection.mutable
import scala.concurrent.{Future, Promise}

/**
  * A callee is a client that register procedures and
  * expects to receive invocations
  */
trait Callee { this: Session =>
  
  private val pendingRegistrations: PendingRegistrations = mutable.Map()

  private val registrations: Registrations = mutable.Map()

  private val pendingUnregisters: PendingUnregisters = mutable.Map()

  private val invocationHandlers: InvocationHandlers = mutable.Map()

  
  /**
    * Register the given procedure so that the given handler will be 
    * executed on invocations.
    *
    * A callee announces the availability of an endpoint implementing a
    * procedure with a dealer by sending a REGISTER message:
    *
    * {{{
    *   ,------.          ,------.               ,------.
    *   |Caller|          |Dealer|               |Callee|
    *   `--+---'          `--+---'               `--+---'
    *      |                 |                      |
    *      |                 |                      |
    *      |                 |       REGISTER       |
    *      |                 | <---------------------
    *      |                 |                      |
    *      |                 |  REGISTERED or ERROR |
    *      |                 | --------------------->
    *      |                 |                      |
    *      |                 |                      |
    *      |                 |                      |
    *      |                 |                      |
    *      |                 |                      |
    *      |                 |      UNREGISTER      |
    *      |                 | <---------------------
    *      |                 |                      |
    *      |                 | UNREGISTERED or ERROR|
    *      |                 | --------------------->
    *   ,--+---.          ,--+---.               ,--+---.
    *   |Caller|          |Dealer|               |Callee|
    *   `------'          `------'               `------'
    * }}}
    *
    * @param procedure is the procedure the callee wants to register
    * @param options is a dictionary that allows to provide additional registration request details in a extensible way
    * @param handler is the handler executed on invocations
    * @return the (future of) registration
    */
  def register(procedure: Uri, options: Dict = Register.defaultOptions)(handler: InvocationHandler): Future[Registered] = {
    withPromise[Registered] { promise =>
      val msg = Register(requestId = nextId(), options, procedure)
      pendingRegistrations += (msg.requestId -> PendingRegistration(msg, handler, promise))
      connection ! msg
    }
  }
  

  private[client] def handleRegistrationSuccess: Receive = {
    case msg @ Registered(requestId, registrationId) =>
      log.debug("<-- {}", msg)
      pendingRegistrations.get(requestId).map { case pending =>
        registrations += (registrationId -> Registration(pending.register.procedure, msg))
        invocationHandlers += (msg.registrationId -> pending.handler)
        pendingRegistrations -= requestId
        pending.promise.success(msg)
      }
  }

  protected def handleRegistrationError: Receive = {
    case msg @ Error(Register.tpe, requestId, _, error, _) =>
      log.debug("<-- {}", msg)
      pendingRegistrations.get(requestId).map { pending =>
        pendingRegistrations -= requestId
        pending.promise.failure(new SessionException(error))
      }
  }
  
  
  // ~~~~~~~~~~~~~~~~~~~~~~~


  // TODO def unregister(procedure: Uri): Future[Unregistered] = {
  
  
  
  // ~~~~~~~~~~~~~~~~~~~~~~~

  protected def handleInvocation: Receive = {
    case msg @ Invocation(_, registrationId, _, _) =>
      log.debug("<-- {}", msg)
      invocationHandlers.get(registrationId) match {
        case Some(handler) => handler(msg)
        case None => log.warn("!!! invocation handler not found for registrationId {}", registrationId)
      }
  }
}



case class Registration(procedure: Uri, registered: Registered)

private case class PendingRegistration(register: Register, handler: InvocationHandler, promise: Promise[Registered])
