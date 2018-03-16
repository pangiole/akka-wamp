package akka.wamp.client.japi

import java.{util => ju}

import akka.wamp.messages.{DataConveyor => DataConveyorDelegate}

import scala.collection.JavaConverters._
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
  *   List<Object> args = message.args();
  *   Map<String, Object> kwargs = message.kwargs();
  *   UserType user = message.kwargs(UserType.class);
  *
  *   // Custom deserializers
  *   Payload payload = message.payload();
  *   // ...
  * }}}
  *
  * @note Java API
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
  def args: ju.List[Any] = delegate.args.asJava

  /**
    * Deserializes named arguments from the conveyed payload to an hashmap
    *
    * @return the (future of) named args
    */
  def kwargs: ju.Map[String, Any] = delegate.kwargs.asJava

  /**
    * Deserializes named arguments from the conveyed payload to a user type
    *
    * @tparam T is the user type
    * @return the (future of) kwargs
    */
  def kwargs[T](clazz: Class[T]): T = delegate.kwargs[T](ClassTag(clazz))
}
