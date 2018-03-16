package akka.wamp

import akka.wamp.client.{Registration, Subscription}
import akka.wamp.messages.Result

import scala.concurrent.Future


package object macros {

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



  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  /**
    * Publish to a topic (fire and forget)
    *
    * @param topic is the topic to publish to
    */
  def publish(topic: Uri)(implicit session: client.Session): Unit =  {
    session.publish(topic)
  }

  /**
    * Publish to a topic (fire and forget)
    *
    * @param topic is the topic to publish to
    * @param args is the list of indexed arguments to be published
    */
  def publish(topic: Uri, args: List[Any])(implicit session: client.Session): Unit =  {
    session.publish(topic, args)
  }


  /**
    * Publish to a topic (fire and forget)
    *
    * @param topic is the topic to publish to
    * @param kwargs is the dictionary of named arguments to be published
    */
  def publish(topic: Uri, kwargs: Map[String, Any])(implicit session: client.Session): Unit =  {
    session.publish(topic, kwargs)
  }



  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  /**
    * Calls the given procedure
    *
    * @param procedure is the procedure to be called
    * @param session is the session
    * @return the (future of) result
    */
  def call(procedure: Uri)(implicit session: client.Session): Future[Result] = {
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
  def call(procedure: Uri, args: List[Any])(implicit session: client.Session): Future[Result] = {
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
  def call(procedure: Uri, kwargs: Map[String, Any])(implicit session: client.Session): Future[Result] = {
    session.call(procedure, kwargs)
  }

  /**
    * Subscribe a 0-parameters lambda handler as the given procedure.
    *
    * @param procedure is the procedure to register
    * @param lambda is the lambda handler to register
    * @return the (future) of registration
    */
  def register[R](procedure: Uri, lambda: Function0[R]): Future[Registration] = macro Macros.register0_impl[R]

  /**
    * Subscribe a 1-parameter lambda handler as the given procedure.
    *
    * @param procedure is the procedure to register
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
