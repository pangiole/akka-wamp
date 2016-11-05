package akka.wamp.client.japi

import java.util.concurrent.CompletionStage

import akka.wamp.client.{Subscription => SubscriptionDelegate}

import scala.compat.java8.FutureConverters.{toJava => asJavaFuture}
import scala.concurrent.ExecutionContext

/**
  * Represents an acknowledged subscription.
  *
  * Instance are obtained by invoking the ''Session.subscribe'' method
  *
  * {{{
  *   CompletionState<Subscription> subscription =
  *     session.thenCompose( s ->
  *       s.subscribe("mytopic", event -> {
  *         // consume event ...
  *       });
  *     );
  *
  *   subscription.whenComplete((sb, ex) -> {
  *     if (sb != null)
  *       log.info("Subscribed to {} with {}", sb.topic(), sb.id());
  *     else
  *       log.warning("Not subscribed because of {}", ex.getMessage)
  *    });
  * }}}
  *
  * @note Java API
  * @see [[akka.wamp.client.Subscription]]
  */
class Subscription private[japi](delegate: SubscriptionDelegate)(implicit ec: ExecutionContext) {


  /** Is the subscription identifier as sent from the router */
  val id = delegate.id

  /** Is the subscribed topic */
  val topic = delegate.topic


  /**
    * Unsubscribes this subscription
    *
    * @return a (future of) unsubscribed acknowledge
    */
  def unsubscribe(): CompletionStage[Unsubscribed] = asJavaFuture {
    delegate.unsubscribe().map(u => new Unsubscribed(u))
  }
}
