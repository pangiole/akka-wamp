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
  
  private val pendingRegisters: PendingRegistrations = mutable.Map()

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
  def register(procedure: Uri, options: Dict = Register.defaultOptions)(handler: InvocationHandler): Future[Registration] = {
    withPromise[Registration] { promise =>
      val msg = Register(requestId = nextId(), options, procedure)
      pendingRegisters += (msg.requestId -> PendingRegistration(msg, handler, promise))
      connection ! msg
    }
  }
  

  private[client] def handleRegistrations: Receive = {
    case msg @ Registered(requestId, registrationId) =>
      log.debug("<-- {}", msg)
      pendingRegisters.get(requestId).map { case pending =>
        val registration = Registration(pending.register.procedure, msg)
        registrations += (registrationId -> registration)
        invocationHandlers += (msg.registrationId -> pending.handler)
        pendingRegisters -= requestId
        pending.promise.success(registration)
      }
      
    case msg @ Error(Register.tpe, requestId, _, error, _) =>
      log.debug("<-- {}", msg)
      pendingRegisters.get(requestId).map { pending =>
        pendingRegisters -= requestId
        pending.promise.failure(new SessionException(error))
      }
      
    case msg @ Unregistered(requestId) =>
      log.debug("<-- {}", msg)
      pendingUnregisters.get(requestId).map {
        case (Unregister(_, registrationId), promise) =>
          registrations -= registrationId
          invocationHandlers -= registrationId
          pendingUnregisters -= requestId
          promise.success(msg)
      }

    case msg @ Error(Unregister.tpe, requestId, _, error, _) =>
      log.debug("<-- {}", msg)
      pendingUnregisters.get(requestId).map {
        case (_, promise) =>
          pendingUnregisters -= requestId
          promise.failure(new SessionException(error))
      }
  }
  
  
  // ~~~~~~~~~~~~~~~~~~~~~~~


  /**
    * Unregister from the given topic
    *
    * @param procedure is the procedure to unregister
    * @return a (future of) unregistered
    */
  def unregister(procedure: Uri): Future[Unregistered] = {
    registrations.find { case (_, registration) =>  registration.procedure == procedure } match {
      case Some((registrationId, _)) => {
        withPromise[Unregistered] { promise =>
          val msg = Unregister(requestId = nextId(), registrationId)
          pendingUnregisters += (msg.requestId -> (msg, promise))
          connection ! msg
        }
      }
      case None =>
        Future.failed[Unregistered](new SessionException("akka.wamp.error.no_such_procedure"))
    }
  }


  // ~~~~~~~~~~~~~~~~~~~~~~~

  protected def handleInvocations: Receive = {
    case msg @ Invocation(_, registrationId, _, _) =>
      log.debug("<-- {}", msg)
      invocationHandlers.get(registrationId) match {
        case Some(handler) => handler(msg)
        case None => log.warn("!!! invocation handler not found for registrationId {}", registrationId)
      }
  }
}



case class Registration(procedure: Uri, registered: Registered) {
  // TODO def unregister(): Future[Unregisterd]
}

private[client] case class PendingRegistration(register: Register, handler: InvocationHandler, promise: Promise[Registration])
