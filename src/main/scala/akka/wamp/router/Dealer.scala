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
  /**
    * Map of procedures registrations. 
    * Each entry is for one procedure only provided by one callee only
    */
  private[router] val registrations = mutable.Map.empty[Id, Registration]

  /**
    * Handle registration lifecycle messages such as: 
    * REGISTER and UNREGISTER
    */
  private[router] def handleRegistrations: Receive = {
    case msg @ Register(requestId, options, procedure) =>
      ifSession(msg, sender()) { session =>
        if (session.roles.contains(Roles.callee)) {
          registrations.values.toList.filter(_.procedure == procedure) match {
            case Nil => {
              /**
                * No callees have registered to provide the given procedure yet.
                */
              val registrationId = scopes('router).nextId(excludes = registrations.toMap.keySet)
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
        } else {
          session.peer ! Error(Register.tpe, requestId, error = "akka.wamp.error.no_callee_role")
        }
      }

    case msg @ Unregister(requestId, registrationId) =>
      ifSession(msg, sender()) { session =>
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
    case msg @ Invocation(requestId, registrationId, details, payload) =>
      ifSession(msg, sender()) { session =>
        
        // TODO stay DRY with "announced roles" checkings 
        
        if (session.roles.contains(Roles.caller)) {

        } else {
          session.peer ! Error(Call.tpe, requestId, error = "akka.wamp.error.no_callee_role")
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

