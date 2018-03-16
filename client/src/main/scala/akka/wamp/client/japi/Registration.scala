package akka.wamp.client.japi

import java.util.concurrent.CompletionStage

import akka.wamp.client.{Registration => RegistrationDelegate}

import scala.compat.java8.FutureConverters.{toJava => asJavaFuture}
import scala.concurrent.ExecutionContext

/**
  * Represents an acknowledged registration.
  *
  * Instance are obtained by invoking the '''Session.register''' method
  *
  * {{{
  *   CompletionState<Subscription> registration =
  *     session.thenCompose( s ->
  *       s.register("myprocedure", invocation -> {
  *         // handle invocation ...
  *         // return future of payload ...
  *       });
  *     );
  *
  *   registration.whenComplete((rg, ex) -> {
  *     if (rg != null)
  *       log.info("Registerd {} with {}", rg.procedure(), rg.id());
  *     else
  *       log.warning("Not registered because of {}", ex.getMessage)
  *    });
  * }}}
  *
  * @note Java API
  * @see [[akka.wamp.client.Registration]]
  */
class Registration private[japi](delegate: RegistrationDelegate)(implicit ec: ExecutionContext) {

  /** Is the subscription identifier as sent from the router */
  val id = delegate.id

  /** Is the registered procedure */
  val procedure = delegate.procedure


  /**
    * Unregisters this registrations
    *
    * @return a (future of) unregister acknowledge
    */
  def unregister(): CompletionStage[Unregistered] = asJavaFuture {
    delegate.unregister().map(u => new Unregistered(u))
  }
}
