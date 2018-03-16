package akka.wamp.client

import akka.wamp.Uri
import akka.wamp.messages.{Event, Subscribed, Unsubscribed}

import scala.concurrent.Future

/**
  * Represents an acknowledged subscription.
  *
  * Instance are obtained by invoking the '''Session.subscribe''' method
  *
  * {{{
  *   val subscription: Future[Subscription] =
  *     session.flatMap { s =>
  *       s.subscribe("mytopic", event => {
  *         // consume event ...
  *       })
  *     }
  *
  *   subscription.onComplete {
  *     case Success(sb) =>
  *       log.info("Subscribed to {} with {}", sb.topic, sb.id)
  *     case Failure(ex) =>
  *       log.warning("Not subscribed because of {}", ex.getMessage)
  *   }
  * }}}
  *
  * @see [[akka.wamp.client.japi.Subscription]]
  */
class Subscription private[client] (
  tpc: Uri,
  session: Session,
  private[client] val consumer: (Event) => Unit,
  private[client] val ack: Subscribed) {

  /** Is the subscription identifier as sent from the router */
  val id = ack.subscriptionId

  /** Is the subscribed topic */
  val topic: Uri = tpc

  /**
    * Unsubscribes this subscription
    *
    * @return a (future of) unsubscribed acknowledge
    */
  def unsubscribe(): Future[Unsubscribed] = session.unsubscribe(ack.subscriptionId)
}