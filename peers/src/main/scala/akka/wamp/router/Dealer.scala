package akka.wamp.router

import akka.actor.ActorRef
import akka.wamp._
import akka.wamp.client.Client
import akka.wamp.messages._
import scala.collection.mutable

/**
  * A Dealer routes invocations incoming from clients with callers to 
  * clients with callees that are registered to provide respective procedures
  */
trait Dealer { this: Router =>
  import Dealer._
  
  /**
    * Map of registrations. 
    * 
    * Each entry represented a specific procedure provided by a specific callee
    * 
    * {{{
    *   requestId -> registration(procedure)
    * }}}
    */
  private[router] val registrations = mutable.Map.empty[Id, Registration]

  /**
    * Map of outstanding invocations.
    *
    * The execution of remote procedure calls is asynchronous, and there
    * may be more than one call outstanding.  A call is called outstanding
    * (from the point of view of the caller), when a (final) result or
    * error has not yet been received by the caller.
    * 
    * {{{
    *   requestId -> invocation(caller, call)
    * }}}
    *
    * The requestId is a random, ephemeral identifier chosen by this dealer 
    * and used to correlate the callee's YIELD response with the INVOCATION 
    * request that was sent.
    * 
    * The outstanding invocation object holds the original CALL.requestId 
    * to be replied with the RESULT message
    */
  private[router] val invocations = mutable.Map.empty[Id, OutstandingInvocation]

  /**
    * Handle registration lifecycle messages such as: 
    * REGISTER and UNREGISTER
    */
  private[router] def handleRegistrations: Receive = {
    case msg @ Register(requestId, options, procedure) =>
      withSession(msg, sender(), Some("callee")) { session =>
        registrations.values.toList.filter(_.procedure == procedure) match {
          case Nil => {
            /**
              * No callees have registered to provide the given procedure yet.
              */
            val registrationId = scopes('router).nextRequestId(excludes = registrations.toMap.keySet)
            registrations += (registrationId -> new Registration(registrationId, session.peer, procedure))
            session.peer ! Registered(requestId, registrationId)
          }
          case registration :: Nil => {
            if (registration.callee != session.peer) {
              /**
                * In case of receiving a REGISTER message from a callee2 
                * providing the procedure already registered some other callee1, 
                * this dealer replies ERROR("wamp.error.procedure_already_exists")
                */
              session.peer ! Error(Register.tpe, requestId, Error.defaultDetails, "wamp.error.procedure_already_exists")
            }
            else {
              /**
                * In case of receiving a REGISTER message from the same callee 
                * providing the already registered procedure, this dealer just
                * confirms the existing registration.
                */
              session.peer ! Registered(requestId, registration.id)
            }
          }
          case _ => {
            log.warning("[{}] !!! IllegalStateException: more than one registration for procedure {} found.", self.path.name, procedure)
          }
        }
      }

    case msg @ Unregister(requestId, registrationId) =>
      withSession(msg, sender(), Some("callee")) { session =>
        registrations.get(registrationId) match {
          case Some(registration) =>
            if (unregister(session.peer, registration)) {
              session.peer ! Unregistered(requestId)
            }
            else {
              // though the given registrationId does exist
              // it turned out to be for some other client
              session.peer ! Error(Unregister.tpe, requestId, error = "wamp.error.no_such_registration")
            }
          case None =>
            // the the given registrationId does NOT exist
            session.peer ! Error(Unregister.tpe, requestId, error = "wamp.error.no_such_registration")
        }
      }  
  }

  /**
    * Handle remote procedure call lifecycle messages such as: 
    * CALL and YIELD
    */
  private[router] def handleCalls: Receive = {
    case call: Call =>
      val caller = sender()
      withSession(call, caller, Some("caller")) { _ =>
        registrations.values.find(_.procedure == call.procedure) match {
          case Some(registration) =>
            /**
              *   ,------.          ,------.          ,------.
              *   |Caller|          |Dealer|          |Callee|
              *   `--+---'          `--+---'          `--+---'
              *      |       CALL      |                 |
              *      | ---------------->                 |
              *      |                 |                 |
              *      |                 |    INVOCATION   |
              *      |                 | ---------------->
              *      |                 |                 |
              *      |                 |  YIELD or ERROR |
              *      |                 | <----------------
              *      |                 |                 |
              *      | RESULT or ERROR |                 |
              *      | <----------------                 |
              *   ,--+---.          ,--+---.          ,--+---.
              *   |Caller|          |Dealer|          |Callee|
              *   `------'          `------'          `------'
              */
            
            // send the INVOCATION message with a different requestId
            val id = scopes('session).nextRequestId()
            val invocation = Invocation(id, registration.id, Invocation.defaultDetails, call.payload)
            invocations += invocation.requestId -> new OutstandingInvocation(caller, call)
            registration.callee ! invocation
            
          case None =>
            /**
              * No callee is providing the procedure
              */
            caller ! Error(Call.tpe, call.requestId, error = "wamp.error.no_such_procedure")
        }
      }
      
    case yld @ Yield(requestId, options, payload) =>
      val callee = sender()
      withSession(yld, callee, Some("callee")) { session =>
        invocations.get(requestId) match {
          case Some(invocation) =>
            invocation.caller ! Result(invocation.call.requestId, Result.defaultDetails, payload)
            
          case None =>
            // TODO what to do when callee YIELD an unknown requestId?
            ???
          
        }
      }
  }


  /**
    * Remove the given registration.
    *
    * @param client is the client actor reference
    * @param registration is the registration to be removed
    * @return ``true`` if given client was registered as callee, ``false`` otherwise 
    */
  private[router] def unregister(client: ActorRef, registration: Registration): Boolean = {
    if (registration.callee == client) {
      registrations -= registration.id
      true
    }
    else false
  }
}


object Dealer {
  /**
    * An outstanding invocation for an outstanding call waiting to be fulfilled
    *
    * @param caller the caller actor reference to reply RESULT to
    * @param call is the original call message sent by the caller
    */
  class OutstandingInvocation(val caller: ActorRef, val call: Call)
}


