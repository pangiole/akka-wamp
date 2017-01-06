package akka.wamp.client.japi

import java.{util => ju}
import ju.{function => juf}
import ju.concurrent.CompletionStage

import akka.Done
import akka.wamp.Uri
import akka.wamp.client.{Session => SessionDelegate}
import akka.wamp.messages.{Closed => ClosedDelegate, Event => EventDelegate, Invocation => InvocationDelegate}
import akka.wamp.serialization.{Payload => PayloadDelegate}

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters.{toJava => asJavaFuture, toScala => asScalaFuture}
import scala.concurrent.{ExecutionContext, Future}

/**
  * Represents a session attached to a realm.
  *
  * == Open ==
  * Instances can be created by invoking the '''Connection.open''' method.
  * {{{
  *   CompletionStage<Connection> conn = ...
  *   CompletionStage<Session> session = conn.thenCompose(c -> c.open("myrealm"));
  * }}}
  *
  * == Topics ==
  *
  * Once the session is open, you can publish to topics.
  *
  * {{{
  *   // import static java.util.Arrays.asList;
  *   CompletionStage<Publication> publication =
  *     session.thenCompose(s -> {
  *       s.publish("mytopic", asList("paolo", 40));
  *     });
  * }}}
  *
  * Or you can subscribe to topics.
  *
  * {{{
  *   CompletionStage<Subscription> subscription =
  *     session.thenCompose(s -> {
  *       s.subscribe("mytopic", event -> {
  *         Long publicationId = event.publicationId();
  *         Long subscriptionId = event.subscriptionId();
  *         Map<String, Object> details = event.details();
  *
  *         // consume event arguments ...
  *       });
  *     });
  * }}}
  *
  * == Procedures ==
  *
  * Once opened, you can call procedures.
  *
  * {{{
  *   CompletionStage<Result> result =
  *     session.thenCompose(s -> s.call("myprocedure", asList("paolo", 99)));
  *
  *   result.whenComplete((res, ex) -> {
  *     if (res != null) log.info("Result: {}", res);
  *     else log.error(ex.getMessage(), ex);
  *   })
  * }}}
  *
  *
  * Or you can register invocation handlers as procedures.
  *
  * {{{
  *   CompletionStage<Registration> registration =
  *     session.thenCompose(s -> {
  *       s.register("myproc", invoc -> {
  *         Long registrationId = invoc.registrationId();
  *         Map<String, Object> details = invoc.details();
  *
  *         // handle invocation arguments ...
  *         // return outgoing payload ...
  *       });
  *     });
  * }}}
  *
  *
  * == Macros ==
  *
  * Not supported for Java
  *
  * @note Java API
  * @see [[akka.wamp.client.japi.DataConveyor]]
  * @see [[akka.wamp.client.Session]]
  */
class Session private[japi](delegate: SessionDelegate)(implicit ec: ExecutionContext) {


  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  /** Is this session unique identifier as sent by the router */
  val id = delegate.id

  /** Are addtional details about this session as sent by the router */
  val details = delegate.details

  /** Is the realm this session is attached to */
  val realm = delegate.realm



  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  /**
    * Closes this session with ``"wamp.error.close_realm"`` as reason and default empty details.
    *
    * @return the (future of) closed signal
    */
  def close(): CompletionStage[Closed] = doClose(delegate.close())

  /**
    * Closes this session with the given reason and default empty details.
    *
    * @param reason is the reason to close
    * @return the (future of) closed signal
    */
  def close(reason: Uri): CompletionStage[Closed] = doClose(delegate.close(reason))

  /**
    * Closes this session with the given reason and additional details.
    *
    * @param reason is the reason to close
    * @param details are the details to send
    * @return the (future of) closed signal
    */
  def close(reason: Uri, details: ju.Map[String, Object]): CompletionStage[Closed] = doClose(delegate.close(reason, details.asScala.toMap))

  /* Excute delegate and convert to Java future */
  private def doClose(fn: => Future[ClosedDelegate]): CompletionStage[Closed] = asJavaFuture(fn.map(_ => new Closed()))



  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  /**
    * Subscribes the given event consumer to the given topic
    *
    * @param topic is the topic to subscribe to
    * @param consumer is the event consumer which will consume incoming events
    * @return the (future of) subscription
    */
  def subscribe(topic: Uri, consumer: juf.Function[Event, CompletionStage[Done]]): CompletionStage[Subscription] = {
    val delegateConsumer: (EventDelegate) => Future[Done] = (event) => {
      val done: Future[Done] = asScalaFuture(
        consumer.apply(new Event(delegate = event))
      )
      done
    }
    asJavaFuture(
      delegate.subscribe(topic, delegateConsumer).map { subscription =>
        new Subscription(delegate = subscription)
      }
    )
  }



  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  /**
    * Publish to a topic (fire and forget)
    *
    * @param topic is the topic to publish to
    */
  def publish(topic: Uri): Unit =
    delegate.publish(topic)

  /**
    * Publish to a topic (fire and forget)
    *
    * @param topic is the topic to publish to
    * @param args is the list of indexed arguments to be published
    */
  def publish(topic: Uri, args: ju.List[Object]): Unit =
    delegate.publish(topic, args.asScala.toList)

  /**
    * Publish to a topic (fire and forget)
    *
    * @param topic is the topic to publish to
    * @param kwargs is the dictionary of named arguments to be published
    */
  def publish(topic: Uri, kwargs: ju.Map[String, Object]): Unit =
    delegate.publish(topic, kwargs.asScala.toList)


  /**
    * Publish to a topic (and acknowledge)
    *
    * @param topic is the topic to publish to
    * @return the (future of) publication acknowledgment
    */
  def publishAck(topic: Uri): CompletionStage[Publication] = asJavaFuture {
    delegate.publishAck(topic).map(p => new Publication(delegate = p))
  }


  /**
    * Publish to a topic (and acknowledge)
    *
    * @param topic is the topic to publish to
    * @param args is the list of indexed arguments to be published
    * @return the (future of) publication acknowledgment
    */
  def publishAck(topic: Uri, args: ju.List[Object]): CompletionStage[Publication] = asJavaFuture {
    delegate.publishAck(topic, args.asScala.toList).map(p => new Publication(delegate = p))
  }


  /**
    * Publish to a topic (and acknowledge)
    *
    * @param topic is the topic to publish to
    * @param kwargs is the dictionary of named arguments to be published
    * @return the (future of) publication acknowledgment
    */
  def publishAck(topic: Uri, kwargs: ju.Map[String, Object]): CompletionStage[Publication] = asJavaFuture {
    delegate.publishAck(topic, kwargs.asScala.toMap).map(p => new Publication(delegate = p))
  }



  // ~~~~~~~~~~~~~~~~~~~~

  /**
    * Registers the given invocation handler as the given procedure
    *
    * @param procedure is the procedure to register as
    * @param handler is the invocation handler which will handle incoming invocations
    * @return the (future of) registration
    */
  def register(procedure: Uri, handler: juf.Function[Invocation, CompletionStage[Payload]]): CompletionStage[Registration] = {
    val invocationHandler: (InvocationDelegate) => Future[PayloadDelegate] = (invocation) => {
      val payload: Future[Payload] = asScalaFuture(
        handler.apply(new Invocation(delegate = invocation))
      )
      payload.map(p => p.delegate)
    }
    asJavaFuture(
      delegate.register(procedure, invocationHandler).map { registration =>
        new Registration(delegate = registration)
      }
    )
  }



  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  /**
    * Calls the given procedure
    *
    * @param procedure the procedure to be called
    * @return the (future of) result
    */
  def call(procedure: Uri): CompletionStage[Result] = asJavaFuture {
    delegate.call(procedure).map(r => new Result(delegate = r))
  }

  /**
    * Calls the given procedure with the given list of indexed arguments
    *
    * @param procedure the procedure to be called
    * @param args the list of indexed arguments
    * @return the (future of) result
    */
  def call(procedure: Uri, args: ju.List[Object]): CompletionStage[Result] = asJavaFuture{
    delegate.call(procedure, args.asScala.toList).map(r => new Result(delegate = r))
  }

  /**
    * Calls the given procedure with the given dictionary of named arguments
    *
    * @param procedure the procedure to be called
    * @param kwargs the dictionary of named arguments
    * @return the (future of) result
    */
  def call(procedure: Uri, kwargs: ju.Map[String, Object]): CompletionStage[Result] = asJavaFuture{
    delegate.call(procedure, kwargs.asScala.toMap).map(r => new Result(delegate = r))
  }


  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~




  private def asScalaList(args: ju.List[Object]) = args.asScala.toList
  
  private def asScalaMap(kwargs: ju.Map[String, Object]) = kwargs.asScala.toMap

}
