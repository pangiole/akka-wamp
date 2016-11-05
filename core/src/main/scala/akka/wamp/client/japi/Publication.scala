package akka.wamp.client.japi

import akka.wamp.client.{Publication => PublicationDelegate}

/**
  * Represents an acknowledged publication.
  *
  * Instance are obtained by invoking the '''Session.publish''' method
  *
  * {{{
  *   CompletionState<Subscription> publication =
  *     session.thenCompose( s ->
  *       s.publishAck("mytopic", asList("paolo", 40));
  *     );
  *
  *   publication.whenComplete((pb, ex) -> {
  *     if (pb != null)
  *       log.info("Subscribed to {} with {}", pb.topic(), pb.id());
  *     else
  *       log.warning("Not publishd because of {}", ex.getMessage)
  *    });
  * }}}
  *
  * @note Java API
  * @see [[akka.wamp.client.Publication]]
  */
class Publication(delegate: PublicationDelegate) {

  /**
    * Is the publication identifier as sent by the router
    */
  val id = delegate.id

  /** Is the topic this publication refers to */
  val topic = delegate.topic
}

