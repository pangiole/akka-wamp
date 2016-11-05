package akka.wamp.client.japi

import java.util.concurrent.CompletionStage
import java.{util => ju}

import akka.wamp.messages.{DataConveyor => DataConveyorDelegate}

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters.{toJava => asJavaFuture}
import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

/**
  * Represents a message conveying application data.
  *
  * == Incoming conveyors ==
  *
  * {{{
  *   // DataConveyor message = ...
  *
  *   // Default deserializers
  *   CompletionStage<List<Object>> args = message.args();
  *   CompletionStage<Map<String, Object>> kwargs = message.kwargs();
  *   CompletionStage<UserType> user = message.kwargs(UserType.class);
  *
  *   // Custom deserializers
  *   Payload payload = message.payload();
  *   // ...
  * }}}
  *
  * @note Java API
  * @see [[akka.wamp.messages.DataConveyor]]
  */
class DataConveyor private[japi](delegate: DataConveyorDelegate)(implicit executionContext: ExecutionContext) {

  /**
    * Is the payload conveyed by this message
    */
  val payload = new Payload(delegate.payload)

  /**
    * Deserializes indexed arguments from the conveyed payload to a list
    *
    * @return the (future of) indexed args
    */
  def args: CompletionStage[ju.List[Any]] = asJavaFuture(delegate.args.map(seqAsJavaList))

  /**
    * Deserializes named arguments from the conveyed payload to an hashmap
    *
    * @return the (future of) named args
    */
  def kwargs: CompletionStage[ju.Map[String, Any]] = asJavaFuture(delegate.kwargs.map(mapAsJavaMap))

  /**
    * Deserializes named arguments from the conveyed payload to a user type
    *
    * @tparam T is the user type
    * @return the (future of) kwargs
    */
  def kwargs[T](clazz: Class[T]): CompletionStage[T] = asJavaFuture(delegate.kwargs[T](ClassTag(clazz)))
}
