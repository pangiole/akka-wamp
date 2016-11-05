package akka.wamp

import scala.language.experimental.macros

/**
  * Contains classes, traits, types and functions to be used to implement WAMP clients in Scala.
  *
  * Please refer to the official
  * <a href="https://angiolep.github.io/projects/akka-wamp/index.html">Akka Wamp User's Guide</a>
  * published online for further details.
  *
  *
  * == Actors ==
  *
  * Is the low level Client API.
  *
  *
  * == Futures ==
  *
  * Is the high level Client API
  *
  * {{{
  * import akka.actor._
  * import akka.wamp.client._
  * import com.typesafe.config._
  *
  * val config = ConfigFactory.load("my.conf")
  * val system = ActorSystem("myapp", config)
  * val client = Client(system)
  *
  * client.connect("myrouter").foreach { conn =>
  *   conn.open("myrealm").foreach { implicit s =>
  *
  *     publish("mytopic", List("paolo", 99))
  *
  *     subscribe("mytopic", (name: String, age: Int) = {
  *       println(s"$name is $age years old")
  *     }
  *
  *     call("myprocedure", List("paolo", 99))
  *
  *     register("myprocedure", (name: String, age: Int) => {
  *       name.length + age
  *     })
  *   }
  * }
  * }}}
  *
  * == Streams ==
  *
  * TBD
  *
  * @see [[akka.wamp.client.japi]] for Java API
  *
  */
package object client {


  import akka.Done
  import akka.actor._
  import akka.wamp.IdScopes.SessionIdScope
  import akka.wamp.client.Connector._
  import akka.wamp.messages._
  import akka.wamp.serialization.Payload

  import scala.concurrent.{Future, Promise}


  /**
    * Represents a session attached to a realm.
    *
    * == Open ==
    *
    * Instances are created either by invoking the '''Connection.open''' or the '''Client.open''' method
    *
    * {{{
    *   val conn: Future[Connection] = ...
    *   val session: Future[Session] = conn.flatMap(c => c.open("myrealm"))
    * }}}
    *
    * == Topics ==
    *
    * Once the session is open, you can publish to topics.
    *
    * {{{
    *   val publication: Future[Publication] =
    *     session.flatMap { s =>
    *       s.publish("mytopic", List("paolo", 40))
    *     }
    * }}}
    *
    * Or you can subscribe event consumers to topics.
    *
    * {{{
    *   val subscription: Future[Subscription] =
    *     session.flatMap { s =>
    *       s.subscribe("mytopic", event => {
    *         val publicationId = event.publicationId
    *         val subscriptionId = event.subscriptionId
    *         val details = event.details
    *         val payload = event.payload
    *         // consume payload content ...
    *       })
    *     }
    * }}}
    *
    * == Procedures ==
    *
    * Once opened, you can call procedures.
    *
    * {{{
    *   val result: Future[Result] =
    *     session.flatMap(s => s.call("myprocedure", List("paolo", 99)))
    * }}}
    *
    * Or you can register invocation handlers as procedures.
    *
    * {{{
    *   val registration: Future[Registration] =
    *     session.flatMap { s =>
    *       s.register("myprocedure", invocation => {
    *         val registrationId = invocation.registrationId
    *         val details = event.details
    *         val payload = event.payload
    *         // handle payload content ...
    *         // return future of payload ...
    *       })
    *     }
    * }}}
    *
    * == Macros ==
    *
    * You can subscribe/register lambda consumers/handlers to make your code look more ''functional'' ;-)
    *
    * {{{
    *   val subscription: Future[Subscription] =
    *     session.flatMap { implicit s =>
    *
    *       publish("mytopic", List("paolo", 40))
    *
    *       subscribe("mytopic", (name: String, age: Int) => {
    *         println(s"$name is $age years old")
    *       })
    *
    *       register("myprocedure", (name: String, age: Int) => {
    *         name.length + age
    *       })
    *     }
    * }}}
    *
    * @see [[akka.wamp.client]]
    * @see [[akka.wamp.messages.DataConveyor]]
    * @see [[akka.wamp.client.Macros]]
    * @see [[akka.wamp.client.japi.Session]]
    */
  class Session private[client](protected val connector: ActorRef, rlm: Uri, welcome: Welcome)
    extends akka.wamp.Session with SessionIdScope
  {

    /** Is this session unique identifier as sent by the router */
    val id = welcome.sessionId

    /** Are addtional details about this session as sent by the router */
    val details = welcome.details

    /** Is the realm this session is attached to */
    val realm = rlm


    /** If this session has been closed */
    @volatile private[client] var closed = false


    /**
      * Closes this session with ``"wamp.error.close_realm"`` as reason and default empty details.
      *
      * @return the (future of) closed signal
      */
    def close(): Future[Closed] = {
      close(Goodbye.defaultReason, Goodbye.defaultDetails)
    }

    /**
      * Closes this session with the given reason and default empty details.
      *
      * @param reason is the reason to close
      * @return the (future of) closed signal
      */
    def close(reason: Uri): Future[Closed] = {
      close(reason, Goodbye.defaultDetails)
    }

    /**
      * Closes this session with the given reason and additional details.
      *
      * @param reason is the reason to close
      * @param details are the details to send
      * @return the (future of) closed signal
      */
    def close(reason: Uri, details: Dict): Future[Closed] = {
      withPromise[Closed] { promise =>
        connector ! SendGoodbye(promise)
        promise.future
      }
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
      * Subscribes the given event consumer to the given topic
      *
      * @param topic is the topic to subscribe to
      * @param consumer is the event consumer which will consume incoming events
      * @return the (future of) subscription
      */
    def subscribe(topic: Uri, consumer: (Event) => Future[Done]): Future[Subscription] = {
      withPromise[Subscription] { promise =>
        connector ! SendSubscribe(topic, consumer, promise)
      }
    }


    private[client] def unsubscribe(subscriptionId: Id): Future[Unsubscribed] = {
      withPromise[Unsubscribed] { promise =>
        connector ! SendUnsubscribe(subscriptionId, promise)
      }
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
      * Publish to a topic (fire and forget)
      *
      * @param topic is the topic to publish to
      */
    def publish(topic: Uri): Unit =  {
      doPublish(topic, Payload())
    }

    /**
      * Publish to a topic (fire and forget)
      *
      * @param topic is the topic to publish to
      * @param args is the list of indexed arguments to be published
      */
    def publish(topic: Uri, args: List[Any]): Unit =  {
      doPublishAck(topic, Payload(args))
    }

    /**
      * Publish to a topic (fire and forget)
      *
      * @param topic is the topic to publish to
      * @param kwargs is the dictionary of named arguments to be published
      */
    def publish(topic: Uri, kwargs: Map[String, Any]): Unit =  {
      doPublish(topic, Payload(kwargs))
    }


    private def doPublish(topic: Uri, payload: Payload): Unit =  {
      connector ! SendPublish(topic, payload, None)
    }


    /**
      * Publish to a topic (and acknowledge)
      *
      * @param topic is the topic to publish to
      * @return the (future of) publication acknowledgment
      */
    def publishAck(topic: Uri): Future[Publication] =  {
      doPublishAck(topic, Payload())
    }

    /**
      * Publish to a topic (and acknowledge)
      *
      * @param topic is the topic to publish to
      * @param args is the list of indexed arguments to be published
      * @return the (future of) publication acknowledgment
      */
    def publishAck(topic: Uri, args: List[Any]): Future[Publication] = {
      doPublishAck(topic, Payload(args))
    }

    /**
      * Publish to a topic (and acknowledge)
      *
      * @param topic is the topic to publish to
      * @param kwargs is the dictionary of named arguments to be published
      * @return the (future of) publication acknowledgment
      */
    def publishAck(topic: Uri, kwargs: Map[String, Any]): Future[Publication] =  {
      doPublishAck(topic, Payload(kwargs))
    }


    private def doPublishAck(topic: Uri, payload: Payload): Future[Publication] =  {
      withPromise[Publication] { promise =>
        connector ! SendPublish(topic, payload, Some(promise))
      }
    }


    /*def tickle(topic: Uri, initialDelay: FiniteDuration, interval: FiniteDuration): Future[Cancellable] = {
      var count = 0
      Future.successful(scheduler.schedule(initialDelay, interval, new Runnable {
        override def run(): Unit = {
          count = if (count == Int.MaxValue) 0 else count + 1
          publish(topic, List(count))
        }
      }))
    }*/


    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
      * Registers the given invocation handler as the given procedure
      *
      * @param procedure is the procedure to register as
      * @param handler is the invocation handler which will handle incoming invocations
      * @return the (future of) registration
      * @see [[akka.wamp.client]]
      */
    def register(procedure: Uri, handler: (Invocation) => Future[Payload]): Future[Registration] = {
      withPromise[Registration] { promise =>
        connector ! SendRegister(procedure, handler, promise)
      }
    }


    private[client] def unregister(registrationId: Id): Future[Unregistered] = {
      withPromise[Unregistered] { promise =>
        connector ! SendUnregister(registrationId, promise)
      }
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    // TODO https://github.com/angiolep/akka-wamp/issues/35
    // TODO Pending calls must have a configurable timeout

    /**
      * Calls the given procedure
      *
      * @param procedure is the procedure to be called
      * @return the (future of) result
      */
    def call(procedure: Uri): Future[Result] = {
      doCall(procedure, Payload())
    }

    /**
      * Calls the given procedure with the given list of indexed arguments
      *
      * @param procedure is the procedure to be called
      * @param args the list of indexed arguments
      * @return the (future of) result
      */
    def call(procedure: Uri, args: List[Any]): Future[Result] = {
      doCall(procedure, Payload(args))
    }

    /**
      * Calls the given procedure with the given dictionary of named arguments
      *
      * @param procedure is the procedure to be called
      * @param kwargs the dictionary of named arguments
      * @return the (future of) result
      */
    def call(procedure: Uri, kwargs: Map[String, Any]): Future[Result] = {
      doCall(procedure, Payload(kwargs))
    }


    private def doCall(procedure: Uri, payload: Payload): Future[Result] = {
      withPromise[Result] { promise =>
        connector ! SendCall(procedure, payload, promise)
      }
    }



    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /* Execute the given function if this session is still open */
    private def withPromise[T](lambda: (Promise[T]) => Unit): Future[T] = {
      val promise = Promise[T]
      if (closed) promise.failure(ClientException("Session closed", new IllegalStateException))
      else lambda(promise)
      promise.future
    }
  }

  /**
    * Publish to a topic (fire and forget)
    *
    * @param topic is the topic to publish to
    */
  def publish(topic: Uri)(implicit session: Session): Unit = session.publish(topic)


  /**
    * Publish to a topic (fire and forget)
    *
    * @param topic is the topic to publish to
    * @param kwargs is the dictionary of named arguments to be published
    */
  def publish(topic: Uri, kwargs:  Map[String, Any])(implicit session: Session): Unit = session.publish(topic, kwargs)


  /**
    * Publish to a topic (fire and forget)
    *
    * @param topic is the topic to publish to
    * @param args is the list of indexed arguments to be published
    */
  def publish(topic: Uri, args: List[Any])(implicit session: Session): Unit = session.publish(topic, args)

  /**
    * Subscribe a 0-parameters lambda consumer to the given topic.
    *
    * @param topic is the topic to subscribe to
    * @param lambda is the lambda consumer to subscribe
    * @return the (future) of subscription
    */
  def subscribe(topic: Uri, lambda: Function0[Unit]): Future[Subscription] = macro Macros.subscribe0_impl

  /**
    * Subscribe a 1-parameter lambda consumer to the given topic.
    *
    * @param topic is the topic to subscribe to
    * @param lambda is the lambda consumer to subscribe
    * @return the (future) of subscription
    */
  def subscribe[T](topic: Uri, lambda: Function1[T, Unit]): Future[Subscription] = macro Macros.subscribe1_impl[T]

  /**
    * Subscribe a 2-parameters lambda consumer to the given topic.
    *
    * @param topic is the topic to subscribe to
    * @param lambda is the lambda consumer to subscribe
    * @return the (future) of subscription
    */
  def subscribe[T1, T2](topic: Uri, lambda: Function2[T1, T2, Unit]): Future[Subscription] = macro Macros.subscribe2_impl[T1, T2]

  /**
    * Subscribe a 3-parameters lambda consumer to the given topic.
    *
    * @param topic is the topic to subscribe to
    * @param lambda is the lambda consumer to subscribe
    * @return the (future) of subscription
    */
  def subscribe[T1, T2, T3](topic: Uri, lambda: Function3[T1, T2, T3, Unit]): Future[Subscription] = macro Macros.subscribe3_impl[T1, T2, T3]


  /**
    * Subscribe a 4-parameters lambda consumer to the given topic.
    *
    * @param topic is the topic to subscribe to
    * @param lambda is the lambda consumer to subscribe
    * @return the (future) of subscription
    */
  def subscribe[T1, T2, T3, T4](topic: Uri, lambda: Function4[T1, T2, T3, T4, Unit]): Future[Subscription] = macro Macros.subscribe4_impl[T1, T2, T3, T4]



  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  /**
    * Calls the given procedure
    *
    * @param procedure is the procedure to be called
    * @param session is the session
    * @return the (future of) result
    */
  def call(procedure: Uri)(implicit session: Session): Future[Result] = {
    session.call(procedure)
  }

  /**
    * Calls the given procedure with the given list of indexed arguments
    *
    * @param procedure is the procedure to be called
    * @param session is the session
    * @param args the list of indexed arguments
    * @return the (future of) result
    */
  def call(procedure: Uri, args: List[Any])(implicit session: Session): Future[Result] = {
    session.call(procedure, args)
  }

  /**
    * Calls the given procedure with the given dictionary of named arguments
    *
    * @param procedure is the procedure to be called
    * @param session is the session
    * @param kwargs the dictionary of named arguments
    * @return the (future of) result
    */
  def call(procedure: Uri, kwargs: Map[String, Any])(implicit session: Session): Future[Result] = {
    session.call(procedure, kwargs)
  }

  /**
    * Subscribe a 0-parameters lambda handler as the given procedure.
    *
    * @param procedure is the procedure to register as
    * @param lambda is the lambda handler to register
    * @return the (future) of registration
    */
  def register[R](procedure: Uri, lambda: Function0[R]): Future[Registration] = macro Macros.register0_impl[R]

  /**
    * Subscribe a 1-parameter lambda handler as the given procedure.
    *
    * @param procedure is the procedure to register as
    * @param lambda is the lambda handler to register
    * @return the (future) of registration
    */
  def register[T1, R](procedure: Uri, lambda: Function1[T1, R]): Future[Registration] = macro Macros.register1_impl[T1, R]

  /**
    * Subscribe a 2-parameters lambda handler as the given procedure.
    *
    * @param procedure is the procedure to register as
    * @param lambda is the lambda handler to register
    * @return the (future) of registration
    */
  def register[T1, T2, R](procedure: Uri, lambda: Function2[T1, T2, R]): Future[Registration] = macro Macros.register2_impl[T1,T2, R]

  /**
    * Subscribe a 3-parameters lambda handler as the given procedure.
    *
    * @param procedure is the procedure to register as
    * @param lambda is the lambda handler to register
    * @return the (future) of registration
    */
  def register[T1, T2, T3, R](procedure: Uri, lambda: Function3[T1, T2, T3, R]): Future[Registration] = macro Macros.register3_impl[T1,T2, T3, R]

  /**
    * Subscribe a 4-parameters lambda handler as the given procedure.
    *
    * @param procedure is the procedure to register as
    * @param lambda is the lambda handler to register
    * @return the (future) of registration
    */
  def register[T1, T2, T3, T4, R](procedure: Uri, lambda: Function4[T1, T2, T3, T4, R]): Future[Registration] = macro Macros.register4_impl[T1,T2, T3, T4, R]

}
