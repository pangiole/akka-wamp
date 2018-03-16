/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.client

import akka.wamp.Uri
import akka.wamp.messages.Published

/**
  * Is an acknowledged publication.
  *
  * Instance are obtained by invoking the '''Session.publish''' method
  *
  * {{{
  *   val publication: Future[Subscription] =
  *     session.flatMap { s =>
  *       s.publishAck("mytopic", List("paolo", 40))
  *     }
  *
  *   publication.onComplete {
  *     case Success(pb) =>
  *       log.info("Published to {} with {}", pb.topic, pb.id)
  *     case Failure(ex) =>
  *       log.warning("Not published because of {}", ex.getMessage)
  *   }
  * }}}
  *
  * @see [[akka.wamp.client.japi.Publication]]
  */
class Publication private[client] (
  tpc: Uri,
  private[client] val ack: Published) {

  /**
    * Is the publication identifier as sent by the router
    */
  val id = ack.publicationId

  /** Is the topic this publication refers to */
  val topic = tpc
}
