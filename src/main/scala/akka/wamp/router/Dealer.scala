package akka.wamp.router

import akka.wamp._
import akka.wamp.client.Client
import akka.wamp.messages.{Error, Invocation, Register, Registered}

/**
  * A Dealer routes [[Invocation]]s incoming from [[Client]]s with [[Roles.caller]]s to 
  * [[Client]]s with [[Roles.callee]]s that are registered to provide respective [[Procedure]]s
  */
trait Dealer { this: Router =>
  /**
    * Map of procedures registrations. 
    * Each entry is for one procedure only provided by one callee only
    */
  private[router] var registrations = Map.empty[Id, Registration]

  /**
    * Handle registration lifecycle messages such as: 
    * REGISTER and UNREGISTER
    */
  private[router] def handleRegistrations: Receive = {
    case message @ Register(requestId, options, procedure) =>
      ifSessionOpen(message) { session =>
        // TODO val callee = session.client
        if (session.roles.contains(Roles.callee)) {
          registrations.values.toList.filter(_.procedure == procedure) match {
            case Nil => {
              /**
                * No callees have registered to provide the given procedure yet.
                */
              val registrationId = scopes('router).nextId(excludes = registrations.keySet)
              registrations += (registrationId -> new Registration(registrationId, session.client, procedure))
              session.client ! Registered(requestId, registrationId)
            }
            case registration :: Nil => {
              if (registration.callee != session.client) {
                /**
                  * In case of receiving a REGISTER message from a callee providing the 
                  * procedure already registered by others, dealer should reply ERROR
                  */
                session.client ! Error(Register.tpe, requestId, Error.defaultDetails, "wamp.error.procedure_already_exists")
                registrations += (registration.id -> registration.copy(callee = session.client))
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
        }
      }
  }
}

