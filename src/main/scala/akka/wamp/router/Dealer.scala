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
    case message @ Register(requestId, options, procedure) =>
      ifSessionOpen(message) { session =>
        if (session.roles.contains(Roles.callee)) {
          registrations.values.toList.filter(_.procedure == procedure) match {
            case Nil => {
              /**
                * No callees have registered to provide the given procedure yet.
                */
              val registrationId = scopes('router).nextId(excludes = registrations.toMap.keySet)
              registrations += (registrationId -> new Registration(registrationId, session.client, procedure))
              session.client ! Registered(requestId, registrationId)
            }
            case registration :: Nil => {
              if (registration.callee != session.client) {
                /**
                  * In case of receiving a REGISTER message from a callee2 
                  * providing the procedure already registered some other callee1, 
                  * this dealer replies ERROR("wamp.error.procedure_already_exists")
                  */
                session.client ! Error(Register.tpe, requestId, Error.defaultDetails, "wamp.error.procedure_already_exists")
              }
              else {
                /**
                  * In case of receiving a REGISTER message from the same callee 
                  * providing the already registered procedure, dealer should answer with 
                  * REGISTERED message, containing the existing registration ID.
                  */
                session.client ! Registered(requestId, registration.id)
              }
            }
            case _ => {
              log.warning("[{}] !!! IllegalStateException: more than one registration for procedure {} found.", self.path.name, procedure)
            }
          }
        } else {
          // TODO Lack of specification! What to do? File an issue on GitHub
          session.client ! Error(Register.tpe, requestId, details = Error.defaultDetails, "akka.wamp.error.no_callee_role")
        }
      }

    case message @ Unregister(requestId, registrationId) =>
      ifSessionOpen(message) { session =>
        registrations.get(registrationId) match {
          case Some(registration) =>
            if (unregister(session.client, registration)) session.client ! Unregistered(requestId)
            else () // TODO Lack of specification! What to reply if session.client was NOT the callee?
          case None =>
            session.client ! Error(Unregister.tpe, requestId, Error.defaultDetails, "wamp.error.no_such_registration")
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

