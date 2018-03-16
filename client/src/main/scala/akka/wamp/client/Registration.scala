/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.client

import akka.wamp.Uri
import akka.wamp.messages.{Invocation, Registered, Unregistered}
import akka.wamp.serialization.Payload

import scala.concurrent.Future



/**
  * Represents an acknowledged registration.
  *
  * Instance are obtained by invoking the '''Session.register''' method
  *
  * {{{
  *   val registration: Future[Registration] =
  *     session.flatMap { s =>
  *       s.register("myprocedure", invocation => {
  *         // handle invocation ...
  *         // return future of payload ...
  *       })
  *     }
  *
  *   registration.onComplete {
  *     case Success(rg) =>
  *       log.info("Registered {} with {}", rg.procedure, rg.id)
  *     case Failure(ex) =>
  *       log.warning("Not registered because of {}", ex.getMessage)
  *   }
  * }}}
  *
  * @see [[akka.wamp.client.japi.Registration]]
  */
class Registration private[client](
  prcdr: Uri,
  session: Session,
  private[wamp] val handler: (Invocation) => Any,
  private[wamp] val ack: Registered) {

  /** Is the registration identifier */
  val id = ack.registrationId

  /** Is the procedure registered */
  val procedure = prcdr

  /**
    * Unregister this registration
    *
    * @return a (future of) unregistered message
    */
  def unregister(): Future[Unregistered] = session.unregister(ack.registrationId)
}
