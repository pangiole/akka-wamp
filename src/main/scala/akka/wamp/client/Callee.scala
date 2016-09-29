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
  
  import Callee._
  
  private val pendingRegistrations: mutable.Map[RequestId, PendingRegistration] = mutable.Map()

  private val registrations: mutable.Map[RegistrationId, Registration] = mutable.Map()
  
  private val pendingUnregistrations: mutable.Map[RequestId, PendingUnregistration] = mutable.Map()

  
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
    * @param handler is the handler executed on invocations
    * @return the (future of) registration
    */
  def register(procedure: Uri)(handler: InvocationHandler): Future[Registration] = {
    withPromise[Registration] { promise =>
      val msg = Register(requestId = nextId(), Register.defaultOptions, procedure)
      pendingRegistrations += (msg.requestId -> new PendingRegistration(msg, handler, promise))
      connection ! msg
    }
  }
  

  private[client] def handleRegistrations: Receive = {
    case msg @ Registered(requestId, registrationId) =>
      log.debug("<-- {}", msg)
      pendingRegistrations.get(requestId).map { case pending =>
        val registration = new Registration(pending.msg.procedure, pending.handler, msg)
        registrations += (registrationId -> registration)
        pendingRegistrations -= requestId
        pending.promise.success(registration)
      }
      
    case msg @ Error(Register.tpe, requestId, _, error, _) =>
      log.debug("<-- {}", msg)
      pendingRegistrations.get(requestId).map { pending =>
        pendingRegistrations -= requestId
        pending.promise.failure(new SessionException(error))
      }
      
    case msg @ Unregistered(requestId) =>
      log.debug("<-- {}", msg)
      pendingUnregistrations.get(requestId).map { pending =>
          registrations -= pending.msg.registrationId
          pendingUnregistrations -= requestId
          pending.promise.success(msg)
      }

    case msg @ Error(Unregister.tpe, requestId, _, error, _) =>
      log.debug("<-- {}", msg)
      pendingUnregistrations.get(requestId).map { pending =>
          pendingUnregistrations -= requestId
          pending.promise.failure(new SessionException(error))
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
    withPromise[Unregistered] { promise =>
      registrations.find { case (_, registration) =>  registration.procedure == procedure } match {
        case Some((registrationId, _)) => {
          val msg = Unregister(requestId = nextId(), registrationId)
          pendingUnregistrations += (msg.requestId -> new PendingUnregistration(msg, promise))
          connection ! msg
        }
        case None =>
          Future.failed[Unregistered](new SessionException("akka.wamp.error.no_such_procedure"))
      }
    }
  }


  // ~~~~~~~~~~~~~~~~~~~~~~~

  
  protected def handleInvocations: Receive = {
    case msg @ Invocation(requestId, registrationId, _, _) =>
      log.debug("<-- {}", msg)
      registrations.get(registrationId) match {
        case Some(registration) => 
          val payload = registration.handler(msg)
          payload.map { p => 
            connection ! Yield(requestId, payload = p) 
          }
          
        case None => 
          log.warn("!!! invocation handler not found for registrationId {}", registrationId)
      }
  }
}


object Callee {
  
  private class PendingRegistration(val msg: Register, val handler: InvocationHandler, val promise: Promise[Registration])
  
  private class PendingUnregistration(val msg: Unregister, val promise: Promise[Unregistered])
}

